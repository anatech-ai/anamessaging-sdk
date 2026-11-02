import { HttpClient, toFormData } from '../http';
import { ApiResponse, MessageDispatchResult } from '../models/common';
import {
  BulkDispatchSummary,
  FileAttachment,
  SendMediaRequest,
  SendTemplateRequest,
  SendTextRequest,
  SendWhatsAppBulkRequest,
  SendWhatsAppRequest,
} from '../models/whatsapp';

/** WhatsApp sends. Every call returns once the message is **queued**, not delivered. */
export class WhatsAppResource {
  constructor(private readonly http: HttpClient) {}

  /** Plain text message. Only within the 24h customer-service window. */
  sendText(request: SendTextRequest): Promise<ApiResponse<MessageDispatchResult>> {
    return this.send({ to: request.to, text: request.text, campaignId: request.campaignId });
  }

  /** Approved template. Pass `attachment` when the template has an image/video/document header. */
  sendTemplate(request: SendTemplateRequest): Promise<ApiResponse<MessageDispatchResult>> {
    const { attachment, ...body } = request;
    return this.send({ language: 'en', ...body }, attachment);
  }

  /** Image, video, document or audio by public URL or `mediaUploadId`. */
  sendMedia(request: SendMediaRequest): Promise<ApiResponse<MessageDispatchResult>> {
    if (!request.mediaUrl && request.mediaUploadId === undefined) {
      throw new TypeError('sendMedia needs mediaUrl or mediaUploadId');
    }
    return this.send(request);
  }

  /** Raw `POST /send/whatsapp` with any of the three body shapes. */
  send(request: SendWhatsAppRequest, attachment?: FileAttachment): Promise<ApiResponse<MessageDispatchResult>> {
    return attachment
      ? this.http.request('POST', '/send/whatsapp', { form: toFormData({ ...request }, attachment) })
      : this.http.request('POST', '/send/whatsapp', { json: request });
  }

  /** One template to many recipients (`POST /send/whatsapp/bulk`). Not recorded as a campaign. */
  sendBulk(request: SendWhatsAppBulkRequest): Promise<ApiResponse<BulkDispatchSummary>> {
    const { attachment, ...body } = request;
    return attachment
      ? this.http.request('POST', '/send/whatsapp/bulk', { form: toFormData(body, attachment) })
      : this.http.request('POST', '/send/whatsapp/bulk', { json: body });
  }
}
