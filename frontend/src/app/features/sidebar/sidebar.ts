import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { SymbolInfo } from '../../core/models/symbol.model';
import { BinanceApiClient } from '../../core/services/binance-api-client';
import { MarketDataStore } from '../../core/services/market-data.store';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class SidebarComponent implements OnInit {
  private readonly marketData = inject(MarketDataStore);
  private readonly binance = inject(BinanceApiClient);

  readonly searchQuery = signal('');
  readonly symbols = signal<SymbolInfo[]>([]);
  readonly selectedSymbolId = computed(() => this.marketData.selectedSymbol().id);

  readonly filteredSymbols = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) {
      return this.symbols();
    }
    return this.symbols().filter(
      (s) => s.pair.toLowerCase().includes(query) || s.name.toLowerCase().includes(query),
    );
  });

  ngOnInit(): void {
    this.binance.exchangeInfo$.subscribe((symbols) => {
      this.symbols.set(symbols);
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  onSymbolSelect(symbol: SymbolInfo): void {
    this.marketData.selectSymbol(symbol);
  }
}
