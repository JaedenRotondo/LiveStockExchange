import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { SymbolInfo } from '../../core/models/symbol.model';
import { BinanceApiClient } from '../../core/services/binance-api-client';
import { MarketDataStore } from '../../core/services/market-data.store';
import { AuthService } from '../../core/services/auth.service';
import { FavoriteService } from '../../core/services/favorite.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class SidebarComponent implements OnInit {
  private readonly marketData = inject(MarketDataStore);
  private readonly binance    = inject(BinanceApiClient);
  readonly auth               = inject(AuthService);
  readonly favoriteService    = inject(FavoriteService);

  readonly searchQuery      = signal('');
  readonly symbols          = signal<SymbolInfo[]>([]);
  readonly selectedSymbolId = computed(() => this.marketData.selectedSymbol().id);

  // Show only favourites when this is toggled
  readonly showFavoritesOnly = signal(false);

  readonly filteredSymbols = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    let list = this.symbols();

    if (this.showFavoritesOnly() && this.auth.isLoggedIn()) {
      list = list.filter(s => this.favoriteService.isFavorited(s.id));
    }

    if (!query) return list;
    return list.filter(
      s => s.pair.toLowerCase().includes(query) || s.name.toLowerCase().includes(query)
    );
  });

  ngOnInit(): void {
    this.binance.exchangeInfo$.subscribe(symbols => this.symbols.set(symbols));

    // Load favourites if already logged in on page load
    if (this.auth.isLoggedIn()) {
      this.favoriteService.load().subscribe();
    }
  }

  onSearch(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  onSymbolSelect(symbol: SymbolInfo): void {
    this.marketData.selectSymbol(symbol);
  }

  onToggleFavorite(event: Event, symbol: SymbolInfo): void {
    event.stopPropagation(); // don't trigger row click
    if (!this.auth.isLoggedIn()) return;
    this.favoriteService.toggle(symbol.id).subscribe();
  }

  isFavorited(symbolId: string): boolean {
    return this.favoriteService.isFavorited(symbolId);
  }
}