import { API_PREFIX, ResolvedConfig, SDK_VERSION } from './config';
import { AnaMessagingError, ConnectionError, RateLimitError, ServerError, errorFromResponse } from './errors';
import { OAuth2TokenResponse } from './models/oauth';
import { FileAttachment } from './models/whatsapp';

export interface RequestOptions {
  query?: Record<string, string | number | undefined>;
  json?: unknown;
  form?: FormData;
  /** Send bearer + CSRF headers (default true). */
  auth?: boolean;
}

interface CachedToken {
  accessToken: string;
  csrfToken: string;
  /** epoch ms; Infinity for a caller-supplied token. */
  expiresAt: number;
}

/** Refresh this long before the token actually expires. */
const REFRESH_MARGIN_MS = 60_000;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Transport: OAuth token lifecycle (fetch, cache, refresh, retry once on 401), JSON / multipart
 * encoding, error mapping and polite retries on throttling.
 */
export class HttpClient {
  private token?: CachedToken;
  private pendingToken?: Promise<CachedToken>;

  constructor(private readonly config: ResolvedConfig) {
    if (config.accessToken && !(config.clientId && config.clientSecret)) {
      this.token = { accessToken: config.accessToken, csrfToken: config.csrfToken ?? '', expiresAt: Infinity };
    }
  }

  /** Exchanges the client credentials for a bearer + CSRF token (`POST /oauth2`). */
  async fetchToken(): Promise<OAuth2TokenResponse> {
    const { clientId, clientSecret, scope } = this.config;
    if (!clientId || !clientSecret) {
      throw new AnaMessagingError('clientId and clientSecret are required to obtain a token');
    }
    const body: Record<string, string> = { grant_type: 'client_credentials', client_id: clientId, client_secret: clientSecret };
    if (scope) body.scope = scope;
    return this.send<OAuth2TokenResponse>('POST', '/oauth2', { json: body, auth: false });
  }

  /** Current token, fetched or refreshed as needed. Concurrent callers share one fetch. */
  async getToken(): Promise<CachedToken> {
    if (this.token && this.token.expiresAt - REFRESH_MARGIN_MS > Date.now()) return this.token;
    if (!this.pendingToken) {
      this.pendingToken = this.fetchToken()
        .then((t) => {
          const expiresAt = t.expires_at ? Date.parse(t.expires_at) : Date.now() + t.expires_in * 1000;
          this.token = { accessToken: t.access_token, csrfToken: t.csrf_token, expiresAt };
          return this.token;
        })
        .finally(() => {
          this.pendingToken = undefined;
        });
    }
    return this.pendingToken;
  }

  /** Drops the cached token; the next call fetches a new one. */
  invalidateToken(): void {
    if (this.token?.expiresAt !== Infinity) this.token = undefined;
  }

  async request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
    const canRefresh = !!(this.config.clientId && this.config.clientSecret);
    try {
      return await this.send<T>(method, path, options);
    } catch (error) {
      // A token revoked or expired server-side: get a fresh one and try once more.
      if (canRefresh && options.auth !== false && error instanceof AnaMessagingError && error.status === 401) {
        this.invalidateToken();
        return this.send<T>(method, path, options);
      }
      throw error;
    }
  }

  private async send<T>(method: string, path: string, options: RequestOptions): Promise<T> {
    const url = new URL(this.config.baseUrl + API_PREFIX + path);
    for (const [k, v] of Object.entries(options.query ?? {})) if (v !== undefined) url.searchParams.set(k, String(v));

    for (let attempt = 0; ; attempt++) {
      const headers: Record<string, string> = {
        Accept: 'application/json',
        'User-Agent': `anamessaging-sdk-node/${SDK_VERSION}`,
      };
      if (options.auth !== false) {
        const token = await this.getToken();
        headers.Authorization = `Bearer ${token.accessToken}`;
        if (token.csrfToken) headers['x-csrf-token'] = token.csrfToken;
      }
      let body: BodyInit | undefined;
      if (options.form) body = options.form; // fetch sets the multipart boundary
      else if (options.json !== undefined) {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(options.json);
      }

      let response: Response;
      try {
        response = await this.config.fetch(url, {
          method,
          headers,
          body,
          signal: AbortSignal.timeout(this.config.timeoutMs),
        });
      } catch (error) {
        throw new ConnectionError(`Could not reach ANA Messaging at ${url.origin}: ${(error as Error).message}`);
      }

      const text = await response.text();
      let parsed: any = text;
      try {
        parsed = text ? JSON.parse(text) : undefined;
      } catch {
        /* non-JSON body */
      }
      if (response.ok) return parsed as T;

      const error = errorFromResponse(response.status, parsed, response.headers.get('retry-after'));
      // Throttled (refused before anything was queued) or temporarily unavailable: safe to retry.
      const retryable =
        (error instanceof RateLimitError && error.code !== 'QUOTA_EXHAUSTED') ||
        (error instanceof ServerError && response.status === 503);
      if (!retryable || attempt >= this.config.maxRetries) throw error;
      const wait = (error as RateLimitError).retryAfterSeconds;
      await sleep(wait !== undefined ? wait * 1000 : 500 * 2 ** attempt);
    }
  }
}

const MIME_BY_EXTENSION: Record<string, string> = {
  jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', mp4: 'video/mp4', '3gp': 'video/3gpp',
  pdf: 'application/pdf', doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel', xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  txt: 'text/plain', aac: 'audio/aac', mp3: 'audio/mpeg', amr: 'audio/amr', ogg: 'audio/ogg',
};

export function guessContentType(filename: string): string {
  const ext = filename.split('.').pop()?.toLowerCase() ?? '';
  return MIME_BY_EXTENSION[ext] ?? 'application/octet-stream';
}

/** Builds a multipart body: scalar fields as text, arrays/objects as JSON, the file in `file`. */
export function toFormData(fields: Record<string, unknown>, attachment: FileAttachment): FormData {
  const form = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    if (value === undefined || value === null) continue;
    form.append(key, typeof value === 'object' ? JSON.stringify(value) : String(value));
  }
  const type = attachment.contentType ?? guessContentType(attachment.filename);
  const blob =
    attachment.data instanceof Blob ? attachment.data : new Blob([attachment.data as BlobPart], { type });
  form.append('file', blob, attachment.filename);
  return form;
}
