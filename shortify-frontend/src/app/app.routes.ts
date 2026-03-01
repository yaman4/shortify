import { Routes } from '@angular/router';
import { ShortenComponent } from './features/shorten/shorten.component';
import { UrlStatsComponent } from './features/url-stats/url-stats.component';
import { RedirectComponent } from './features/redirect/redirect.component';

export const routes: Routes = [
  { path: '', component: ShortenComponent },
  { path: 'stats/:shortCode', component: UrlStatsComponent },

  // ⚠️ redirect route MUST be last (before wildcard)
  { path: ':shortCode', component: RedirectComponent },

  { path: '**', redirectTo: '' }
];
