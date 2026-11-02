import { HttpClient, toFormData } from '../http';
import { ApiResponse } from '../models/common';
import { CampaignDetail, CampaignList, RunCampaignRequest, RunCampaignResult } from '../models/campaigns';

export class CampaignsResource {
  constructor(private readonly http: HttpClient) {}

  /** Run a recorded campaign (opt-in and 24h rules applied; status readable with `get`). */
  run(request: RunCampaignRequest): Promise<ApiResponse<RunCampaignResult>> {
    const { attachment, ...body } = request;
    return attachment
      ? this.http.request('POST', '/campaigns', { form: toFormData(body, attachment) })
      : this.http.request('POST', '/campaigns', { json: body });
  }

  /** Your campaigns, newest first. `limit` 1-200, default 50. */
  list(params: { limit?: number; offset?: number } = {}): Promise<ApiResponse<CampaignList>> {
    return this.http.request('GET', '/campaigns', { query: params });
  }

  /** One campaign with per-recipient status and skipped recipients. */
  get(campaignId: string | number): Promise<ApiResponse<CampaignDetail>> {
    return this.http.request('GET', `/campaigns/${encodeURIComponent(String(campaignId))}`);
  }
}
