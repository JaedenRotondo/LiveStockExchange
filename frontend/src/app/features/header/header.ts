import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MarketDataStore } from '../../core/services/market-data.store';
import { AuthService } from '../../core/services/auth.service';
import { TIMEFRAMES } from '../../core/constants/chart.constants';
import { Timeframe } from '../../core/models/timeframe.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class HeaderComponent {
  private readonly marketData = inject(MarketDataStore);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly timeframes       = TIMEFRAMES;
  readonly selectedSymbol   = this.marketData.selectedSymbol;
  readonly selectedTimeframe = this.marketData.selectedTimeframe;
  readonly currentQuote     = this.marketData.currentQuote;

  readonly showUserMenu = signal(false);

  onTimeframeSelect(tf: Timeframe): void {
    this.marketData.selectTimeframe(tf);
  }

  toggleUserMenu(): void {
    this.showUserMenu.update(v => !v);
  }

  logout(): void {
    this.showUserMenu.set(false);
    this.auth.logout();
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToHoldings(): void {
    this.router.navigate(['/holdings']);
  }
}