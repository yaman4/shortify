import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';

import { UrlService } from '../../core/services/url.service';

@Component({
  standalone: true,
  selector: 'app-redirect',
  imports: [CommonModule],
  template: `
    <div class="redirect">
      <p *ngIf="loading">Redirecting...</p>
      <p *ngIf="error" class="error">{{ error }}</p>
    </div>
  `,
  styles: [`
    .redirect {
      text-align: center;
      margin-top: 80px;
      font-size: 18px;
    }
    .error {
      color: red;
    }
  `]
})
export class RedirectComponent implements OnInit {

  loading = true;
  error?: string;

  constructor(
    private route: ActivatedRoute,
    private urlService: UrlService
  ) {}

  ngOnInit() {
    const shortCode = this.route.snapshot.paramMap.get('shortCode');

    if (!shortCode) {
      this.error = 'Invalid short URL';
      this.loading = false;
      return;
    }

    this.urlService.resolve(shortCode).subscribe({
      next: res => {
        window.location.href = res.originalUrl;
      },
      error: err => {
        this.loading = false;
        if (err.status === 404) {
          this.error = 'Short URL not found';
        } else if (err.status === 410) {
          this.error = 'Link has expired';
        } else if (err.status === 429) {
          this.error = 'Too many requests. Try later.';
        } else {
          this.error = 'Unable to redirect';
        }
      }
    });
  }
}
