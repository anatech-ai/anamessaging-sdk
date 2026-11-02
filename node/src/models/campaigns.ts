import { FileAttachment, TemplateComponent } from './whatsapp';

/** Body of `POST /api/v1/anamsg/campaigns` - a recorded campaign. */
export interface RunCampaignRequest {
  templateName: string;
  language?: string;
  components?: TemplateComponent[];
  recipients?: string[];
  customerIds?: number[];
  useBulkPubSub?: boolean;
  /** Shown on the portal's Campaigns page (max 200). */
  name?: string;
  attachment?: FileAttachment;
}

export type CampaignSkipReason = 'NO_OPT_IN' | 'SENT_IN_LAST_24H' | 'DUPLICATE' | 'QUEUE_ERROR' | string;

export interface CampaignSkip {
  to: string;
  reason: CampaignSkipReason;
  detail?: string;
}

export interface RunCampaignResult {
  campaignId: string;
  status: string;
  campaignSize: number;
  queued: number;
  skipped: number;
  skippedRecipients: CampaignSkip[];
  failed: number;
  errors: { to: string; error?: string }[];
  category?: string;
}

export interface CampaignResults {
  pending: number;
  sent: number;
  delivered: number;
  read: number;
  failed: number;
}

export interface Campaign {
  id: string;
  name: string;
  templateName: string;
  templateLanguage: string;
  category?: string;
  status: string;
  totalRecipients: number;
  queued: number;
  skipped: number;
  results: CampaignResults;
  createdAt: string;
}

export interface CampaignList {
  total: number;
  campaigns: Campaign[];
}

export interface CampaignRecipient {
  messageId: string | number;
  to: string;
  status: string;
  error: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface CampaignDetail {
  campaign: Campaign;
  recipients: CampaignRecipient[];
  skippedRecipients: CampaignSkip[];
}
