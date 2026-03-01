export interface ShortenRequest {
  originalUrl: string;
  customAlias?: string;
  ttlInSeconds?: number;
}
