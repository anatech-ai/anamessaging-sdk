import { MessageDispatchResult } from './common';

export type MediaType = 'image' | 'video' | 'document' | 'audio';

/** One parameter of a WhatsApp template component (WhatsApp Cloud API format, passed through). */
export interface TemplateParameter {
  type: 'text' | 'currency' | 'date_time' | 'image' | 'video' | 'document' | 'payload' | string;
  text?: string;
  payload?: string;
  image?: { link?: string; id?: string };
  video?: { link?: string; id?: string };
  document?: { link?: string; id?: string; filename?: string };
  currency?: { fallback_value: string; code: string; amount_1000: number };
  date_time?: { fallback_value: string };
  [key: string]: unknown;
}

/** A template component: header, body or button parameters. */
export interface TemplateComponent {
  type: 'header' | 'body' | 'button' | string;
  /** For buttons: `url`, `quick_reply`, ... */
  sub_type?: string;
  /** For buttons: position of the button, as a string ("0", "1", ...). */
  index?: string;
  parameters?: TemplateParameter[];
  [key: string]: unknown;
}

/**
 * A file to attach to a template whose header is an image, video or document.
 * Sent as `multipart/form-data` in the `file` field.
 */
export interface FileAttachment {
  /** File contents. */
  data: Buffer | Uint8Array | Blob | ArrayBuffer;
  /** File name, e.g. `invoice.pdf`. */
  filename: string;
  /** MIME type, e.g. `application/pdf`. Guessed from the extension when omitted. */
  contentType?: string;
}

/**
 * Body of `POST /api/v1/anamsg/send/whatsapp`. Use one of three shapes:
 *
 * | Send     | Fields                                                     |
 * | -------- | ---------------------------------------------------------- |
 * | Text     | `to`, `text`                                               |
 * | Template | `to`, `templateName`, `language`, `components`             |
 * | Media    | `to`, `mediaType`, `mediaUrl` or `mediaUploadId`, `caption`, `filename` |
 */
export interface SendWhatsAppRequest {
  /** Destination number with country code, e.g. `919876543210`. `+` and spaces are stripped. 7-20 chars. */
  to: string;
  /** Text body (max 4096). Required for a text send. */
  text?: string;
  /** Alias for `text`. */
  message?: string;
  /** Approved template name. Required for a template send. */
  templateName?: string;
  /** Template language code. Default `en`. */
  language?: string;
  /** Template header/body/button parameters. */
  components?: TemplateComponent[];
  /** Media kind for a media send. Optional with `mediaUploadId`. */
  mediaType?: MediaType;
  /** Id returned by `media.upload()`. Use instead of `mediaUrl`. */
  mediaUploadId?: number;
  /** Publicly reachable URL of the media to send. */
  mediaUrl?: string;
  /** Caption for an image or video (max 1024). */
  caption?: string;
  /** File name shown to the recipient of a document (max 255). */
  filename?: string;
  /** Your own correlation id (e.g. an ERP document number), max 100. */
  campaignId?: string;
}

/** Text message. */
export interface SendTextRequest {
  to: string;
  text: string;
  campaignId?: string;
}

/** Approved template message. */
export interface SendTemplateRequest {
  to: string;
  templateName: string;
  /** Default `en`. */
  language?: string;
  components?: TemplateComponent[];
  /** Required when the template has an image/video/document header. */
  attachment?: FileAttachment;
  campaignId?: string;
}

/** Media message (image, video, document, audio). Give `mediaUrl` or `mediaUploadId`. */
export interface SendMediaRequest {
  to: string;
  mediaType?: MediaType;
  mediaUrl?: string;
  mediaUploadId?: number;
  caption?: string;
  filename?: string;
  campaignId?: string;
}

/**
 * Body of `POST /api/v1/anamsg/send/whatsapp/bulk`. Recipients: `recipients`, else
 * `customerIds`, else every active customer of your organisation.
 */
export interface SendWhatsAppBulkRequest {
  templateName: string;
  language?: string;
  components?: TemplateComponent[];
  /** Phone numbers (max 10 000). */
  recipients?: string[];
  /** ANA customer ids of your organisation (max 10 000). */
  customerIds?: number[];
  /** Publish the whole run as one event. Not available with an attachment. */
  useBulkPubSub?: boolean;
  campaignId?: string;
  /** Required when the template has an image/video/document header. */
  attachment?: FileAttachment;
}

/** Result of a bulk send. */
export interface BulkDispatchSummary {
  campaignSize: number;
  queued: number;
  failed: number;
  messages: MessageDispatchResult[];
  errors: { to: string; error: string }[];
  pubSubMessageId?: string;
}
