import { HttpClient, guessContentType } from '../http';
import { ApiResponse } from '../models/common';
import { MediaUploadResult } from '../models/media';
import { FileAttachment, MediaType } from '../models/whatsapp';

export class MediaResource {
  constructor(private readonly http: HttpClient) {}

  /**
   * Upload a file once and send it by `mediaUploadId` as often as you like.
   * Limits: image JPG/PNG 5 MB, video MP4 15 MB, audio 16 MB, document 100 MB.
   */
  upload(file: FileAttachment, mediaType?: MediaType): Promise<ApiResponse<MediaUploadResult>> {
    const form = new FormData();
    const type = file.contentType ?? guessContentType(file.filename);
    const blob = file.data instanceof Blob ? file.data : new Blob([file.data as BlobPart], { type });
    form.append('file', blob, file.filename);
    if (mediaType) form.append('mediaType', mediaType);
    return this.http.request('POST', '/media', { form });
  }
}
