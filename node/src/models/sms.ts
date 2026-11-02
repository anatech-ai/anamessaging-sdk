import { MessageDispatchResult } from './common';

/**
 * Body of `POST /api/v1/anamsg/sms/sent`. Give `to` or `recipients`.
 * Indian A2P traffic needs `principalEntityId` and `dltTemplateId`.
 */
export interface SendSmsRequest {
  /** One destination number with country code. */
  to?: string;
  /** Several destination numbers. Takes precedence over `to`. */
  recipients?: string[];
  /** SMS body, 1-1600 characters. */
  message: string;
  /** Approved sender id / mask (max 20). Defaults to your account's sender. */
  senderId?: string;
  /** DLT Principal Entity ID (India). */
  principalEntityId?: string;
  /** DLT template id matching the message body (India). */
  dltTemplateId?: string;
  /** Send as Unicode (non-Latin scripts). Default false. */
  unicode?: boolean;
  /** Your own correlation id, max 100. */
  campaignId?: string;
}

export interface SendSmsResult {
  total: number;
  messages: MessageDispatchResult[];
}
