import { HttpClient } from '../http';
import { ApiResponse } from '../models/common';
import { SendSmsRequest, SendSmsResult } from '../models/sms';

export class SmsResource {
  constructor(private readonly http: HttpClient) {}

  /** Queue an SMS to `to` or to every number in `recipients` (`POST /sms/sent`). */
  send(request: SendSmsRequest): Promise<ApiResponse<SendSmsResult>> {
    if (!request.to && !request.recipients?.length) throw new TypeError('SMS needs `to` or `recipients`');
    return this.http.request('POST', '/sms/sent', { json: request });
  }
}
