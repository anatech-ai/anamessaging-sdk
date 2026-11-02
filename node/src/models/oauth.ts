/** Token issued by `POST /api/v1/anamsg/oauth2` (OAuth 2.0 client credentials). */
export interface OAuth2TokenResponse {
  /** Bearer token, sent as `Authorization: Bearer <token>`. */
  access_token: string;
  /** Always `Bearer`. */
  token_type: string;
  /** Seconds until the token expires. */
  expires_in: number;
  /** `<client id>/<organization slug>-<organization id>`. */
  scope?: string;
  /** CSRF token bound to this access token, sent as `x-csrf-token` on every call. */
  csrf_token: string;
  /** ISO timestamp at which the token expires. */
  expires_at: string;
}
