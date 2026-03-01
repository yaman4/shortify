import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AiService {

  // Frontend can optionally have helpers for UI (AI suggestions)
  isMalicious(url: string): boolean {
    // Optional: can integrate with frontend AI checks if needed
    return false;
  }

  suggestAlias(url: string): string {
    // Simple alias suggestion: take last 6 chars of a hash
    return Math.random().toString(36).substring(2, 8);
  }
}
