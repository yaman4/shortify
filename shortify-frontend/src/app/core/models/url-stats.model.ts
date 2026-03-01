import { RiskLevel } from './risk-level.model';

export interface UrlStats {
  shortCode: string;
  originalUrl: string;
  redirectCount: number;
  aiChecked: boolean;
  riskLevel: RiskLevel;
}
