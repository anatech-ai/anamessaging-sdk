import { AnaMessagingConfig, resolveConfig } from './config';
import { HttpClient } from './http';
import { HealthStatus } from './models/common';
import { OAuth2TokenResponse } from './models/oauth';
import { CampaignsResource } from './resources/campaigns';
import { MediaResource } from './resources/media';
import { SmsResource } from './resources/sms';
import { WhatsAppResource } from './resources/whatsapp';

/**
 * ANA Messaging API client.
 *
 * ```ts
 * const ana = new AnaMessagingClient({ clientId, clientSecret });
 * await ana.whatsapp.sendText({ to: '919876543210', text: 'Hello' });
 * await ana.sms.send({ to: '919876543210', message: 'Your OTP is 1234' });
 * ```
 *
 * Tokens are obtained and refreshed automatically. Requires an active ANA Messaging API
 * subscription; otherwise calls throw `SubscriptionInactiveError`.
 */
export class AnaMessagingClient {
  readonly whatsapp: WhatsAppResource;
  readonly sms: SmsResource;
  readonly campaigns: CampaignsResource;
  readonly media: MediaResource;
  private readonly http: HttpClient;

  constructor(config: AnaMessagingConfig = {}) {
    this.http = new HttpClient(resolveConfig(config));
    this.whatsapp = new WhatsAppResource(this.http);
    this.sms = new SmsResource(this.http);
    this.campaigns = new CampaignsResource(this.http);
    this.media = new MediaResource(this.http);
  }

  /** Fetch a fresh token now - useful to validate credentials and subscription at startup. */
  authenticate(): Promise<OAuth2TokenResponse> {
    return this.http.fetchToken();
  }

  /** API health; needs no credentials. */
  health(): Promise<HealthStatus> {
    return this.http.request('GET', '/health', { auth: false });
  }
}
