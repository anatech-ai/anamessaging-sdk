/** Production API host. */
export const PRODUCTION_BASE_URL = 'https://api.anamessaging.com';
/** Development / sandbox API host. */
export const SANDBOX_BASE_URL = 'https://api-dev.anamessaging.com';
/** Path prefix of every ANA Messaging API route. */
export const API_PREFIX = '/api/v1/anamsg';
export const SDK_VERSION = '1.0.0';

/**
 * Client configuration.
 *
 * The client id and secret are issued on anamessaging.com (Settings -> API Keys) to organisations
 * with an **active API subscription**. Without an active subscription the API refuses every call
 * and the SDK throws `SubscriptionInactiveError`.
 */
export interface AnaMessagingConfig {
  /** Client id issued on anamessaging.com. Env: `ANAMESSAGING_CLIENT_ID`. */
  clientId?: string;
  /** Client secret issued on anamessaging.com (shown once). Env: `ANAMESSAGING_CLIENT_SECRET`. */
  clientSecret?: string;
  /** Optional scope, `<client id>/<org slug>-<org id>`. Env: `ANAMESSAGING_SCOPE`. */
  scope?: string;
  /**
   * Use an already-issued token instead of client credentials (it is not refreshed).
   * Env: `ANAMESSAGING_ACCESS_TOKEN` / `ANAMESSAGING_CSRF_TOKEN`.
   */
  accessToken?: string;
  csrfToken?: string;
  /** API host. Default production. Env: `ANAMESSAGING_BASE_URL`. */
  baseUrl?: string;
  /** Per-request timeout in ms. Default 30 000. */
  timeoutMs?: number;
  /** Retries on 429 (per-second throttle) and 503, honouring `Retry-After`. Default 2. */
  maxRetries?: number;
  /** Custom fetch implementation (default: global fetch, Node 18+). */
  fetch?: typeof fetch;
}

export interface ResolvedConfig {
  clientId?: string;
  clientSecret?: string;
  scope?: string;
  accessToken?: string;
  csrfToken?: string;
  baseUrl: string;
  timeoutMs: number;
  maxRetries: number;
  fetch: typeof fetch;
}

export function resolveConfig(config: AnaMessagingConfig = {}): ResolvedConfig {
  const env = (typeof process !== 'undefined' && process.env) || {};
  const resolved: ResolvedConfig = {
    clientId: config.clientId ?? env.ANAMESSAGING_CLIENT_ID,
    clientSecret: config.clientSecret ?? env.ANAMESSAGING_CLIENT_SECRET,
    scope: config.scope ?? env.ANAMESSAGING_SCOPE,
    accessToken: config.accessToken ?? env.ANAMESSAGING_ACCESS_TOKEN,
    csrfToken: config.csrfToken ?? env.ANAMESSAGING_CSRF_TOKEN,
    baseUrl: (config.baseUrl ?? env.ANAMESSAGING_BASE_URL ?? PRODUCTION_BASE_URL).replace(/\/+$/, ''),
    timeoutMs: config.timeoutMs ?? 30_000,
    maxRetries: config.maxRetries ?? 2,
    fetch: config.fetch ?? (globalThis.fetch as typeof fetch),
  };
  if (!resolved.fetch) {
    throw new Error('ANA Messaging SDK needs a global fetch (Node.js 18+) or a `fetch` option.');
  }
  const hasCredentials = !!(resolved.clientId && resolved.clientSecret);
  if (!hasCredentials && !resolved.accessToken) {
    throw new Error(
      'ANA Messaging SDK: set clientId and clientSecret (issued on anamessaging.com with an active ' +
        'subscription), or accessToken and csrfToken.',
    );
  }
  return resolved;
}
