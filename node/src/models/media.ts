import { MediaType } from './whatsapp';

/** Result of `POST /api/v1/anamsg/media`. Send it with `mediaUploadId`. */
export interface MediaUploadResult {
  mediaUploadId: number;
  mediaType: MediaType;
  mimeType: string;
  filename: string;
  size: number;
}
