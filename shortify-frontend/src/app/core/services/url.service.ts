import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

import { ShortenRequest } from '../models/shorten-request.model';
import { ShortenResponse } from '../models/shorten-response.model';
import { UrlStats } from '../models/url-stats.model';

@Injectable({
  providedIn: 'root'
})
export class UrlService {

  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  constructor(private http: HttpClient) {}

  shorten(request: ShortenRequest): Observable<ShortenResponse> {
    return this.http.post<ShortenResponse>(
      `${this.baseUrl}/shorten`,
      request
    );
  }

  getStats(shortCode: string): Observable<UrlStats> {
    return this.http.get<UrlStats>(
      `${this.baseUrl}/stats/${shortCode}`
    );
  }

  resolve(shortCode: string): Observable<{ originalUrl: string }> {
  return this.http.get<{ originalUrl: string }>(
    `${this.baseUrl}/resolve/${shortCode}`
  );
}

}
