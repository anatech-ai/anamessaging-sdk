/**
 * Envelope every ANA Messaging API response (other than `/oauth2`) is wrapped in.
 */
export interface ApiResponse<T> {
  /** `true` when the call was accepted. */
  success: boolean;
  /** Human-readable summary, e.g. `"WhatsApp message queued for delivery"`. */
  message?: string;
  /** The payload of a successful call. */
  data?: T;
  /** Validation or per-field errors on failure. */
  errors?: string[];
  /** Machine-readable failure reason, e.g. `QUOTA_EXHAUSTED`, `TEMPLATE_NOT_APPROVED`. */
  code?: string;
  /** ISO-8601 server timestamp. */
  timestamp: string;
}

/** Outcome of queuing one message for one recipient. */
export interface MessageDispatchResult {
  /** Id of the message on the ANA platform. Keep it to reconcile delivery webhooks. */
  messageId: number | string;
  /** Normalised destination number, e.g. `919876543210`. */
  to: string;
  /** `queued` = accepted for delivery (not yet delivered); `failed` = not queued, see `error`. */
  status: 'queued' | 'failed' | string;
  /** Platform event id for the queued send. */
  pubSubMessageId?: string;
  /** Why this recipient was not queued. */
  error?: string;
}

/** `GET /health` response. Needs no credentials. */
export interface HealthStatus {
  status: string;
  timestamp: string;
  service: string;
  database: string;
  pubsub: string;
}
