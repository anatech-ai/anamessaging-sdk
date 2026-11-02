/** Base class of every error the SDK throws. */
export class AnaMessagingError extends Error {
  /** HTTP status, or 0 when the API could not be reached. */
  readonly status: number;
  /** Machine-readable reason, e.g. `QUOTA_EXHAUSTED`, `invalid_client`. */
  readonly code?: string;
  /** Field-level validation messages. */
  readonly errors?: string[];
  /** Raw response body. */
  readonly body?: unknown;

  constructor(message: string, status = 0, code?: string, errors?: string[], body?: unknown) {
    super(message);
    this.name = new.target.name;
    this.status = status;
    this.code = code;
    this.errors = errors;
    this.body = body;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** 400 / 413 - the request was rejected; fix the payload. */
export class ValidationError extends AnaMessagingError {}
/** 401 - wrong client id/secret, or the token is invalid, expired or revoked. */
export class AuthenticationError extends AnaMessagingError {}
/** 403 - CSRF, IP allowlist or domain check failed. */
export class PermissionDeniedError extends AnaMessagingError {}
/**
 * 403 - no active ANA Messaging subscription (missing, expired, or API access disabled).
 * Renew on anamessaging.com; the SDK cannot send until then.
 */
export class SubscriptionInactiveError extends PermissionDeniedError {}
/** 404 - e.g. unknown template or campaign. */
export class NotFoundError extends AnaMessagingError {}
/** 429 - too many requests. `retryAfterSeconds` says when to retry. */
export class RateLimitError extends AnaMessagingError {
  retryAfterSeconds?: number;
}
/** 429 `QUOTA_EXHAUSTED` - the subscription's monthly allowance is used up. Upgrade or top up. */
export class QuotaExceededError extends RateLimitError {}
/** 5xx. */
export class ServerError extends AnaMessagingError {}
/** Network failure or timeout. */
export class ConnectionError extends AnaMessagingError {}

const SUBSCRIPTION_CODES = new Set(['NO_SUBSCRIPTION', 'SUBSCRIPTION_EXPIRED']);

/** Builds the right error subclass from an HTTP error response. */
export function errorFromResponse(status: number, body: any, retryAfter?: string | null): AnaMessagingError {
  const code: string | undefined = body?.code ?? body?.error;
  const message: string =
    body?.error_description ?? body?.message ?? (typeof body === 'string' && body) ?? `HTTP ${status}`;
  const errors: string[] | undefined = Array.isArray(body?.errors) ? body.errors : undefined;
  const args = [message, status, code, errors, body] as const;

  if (status === 400 || status === 413 || status === 422) return new ValidationError(...args);
  if (status === 401) return new AuthenticationError(...args);
  if (status === 403) {
    if ((code && SUBSCRIPTION_CODES.has(code)) || /subscription|API access is not enabled/i.test(message)) {
      return new SubscriptionInactiveError(...args);
    }
    return new PermissionDeniedError(...args);
  }
  if (status === 404) return new NotFoundError(...args);
  if (status === 429) {
    const err = code === 'QUOTA_EXHAUSTED' ? new QuotaExceededError(...args) : new RateLimitError(...args);
    const seconds = retryAfter ? Number(retryAfter) : NaN;
    if (!Number.isNaN(seconds)) err.retryAfterSeconds = seconds;
    return err;
  }
  if (status >= 500) return new ServerError(...args);
  return new AnaMessagingError(...args);
}
