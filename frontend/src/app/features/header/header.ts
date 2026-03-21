import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MarketDataStore } from '../../core/services/market-data.store';
import { TIMEFRAMES } from '../../core/constants/chart.constants';
import { Timeframe } from '../../core/models/timeframe.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class HeaderComponent {
  private readonly marketData = inject(MarketDataStore);

  readonly timeframes = TIMEFRAMES;
  readonly selectedSymbol = this.marketData.selectedSymbol;
  readonly selectedTimeframe = this.marketData.selectedTimeframe;
  readonly currentQuote = this.marketData.currentQuote;

  onTimeframeSelect(tf: Timeframe): void {
    this.marketData.selectTimeframe(tf);
  }
}
