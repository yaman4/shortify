import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { UrlService } from '../../core/services/url.service';
import { ShortenResponse } from '../../core/models/shorten-response.model';

@Component({
  standalone: true,
  selector: 'app-shorten',
  imports: [CommonModule, FormsModule],
  templateUrl: './shorten.component.html',
  styleUrls: ['./shorten.component.css']
})
export class ShortenComponent {

  originalUrl = '';
  customAlias = '';
  ttlInSeconds?: number;

  shortUrl?: ShortenResponse;
  error?: string;

  constructor(
    private urlService: UrlService,
    private router: Router
  ) {}

  shorten() {
    this.error = undefined;

    this.urlService.shorten({
      originalUrl: this.originalUrl,
      customAlias: this.customAlias || undefined,
      ttlInSeconds: this.ttlInSeconds
    }).subscribe({
      next: res => {
        this.shortUrl = res;
        this.router.navigate(['/stats', res.shortCode]);
      },
      error: err => {
        this.error = err.error || 'Something went wrong';
      }
    });
  }
}
