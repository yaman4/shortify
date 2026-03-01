import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { UrlService } from '../../core/services/url.service';
import { UrlStats } from '../../core/models/url-stats.model';

@Component({
  selector: 'app-url-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './url-stats.component.html',
  styleUrls: ['./url-stats.component.css']
})
export class UrlStatsComponent {

  stats?: UrlStats;

  constructor(
    private route: ActivatedRoute,
    private urlService: UrlService
  ) {
    const shortCode = this.route.snapshot.paramMap.get('shortCode');
    if (shortCode) {
      this.urlService.getStats(shortCode)
        .subscribe((res: UrlStats) => {
          this.stats = res;
        });
    }
  }
}
