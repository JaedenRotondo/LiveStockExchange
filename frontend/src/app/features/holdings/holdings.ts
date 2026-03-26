import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { HeaderComponent } from '../header/header';
import { HoldingService } from '../../core/services/holding.service';
import { TransactionService } from '../../core/services/transaction.service';
import { MarketDataStore } from '../../core/services/market-data.store';
import { Holding, Transaction, AddTransactionRequest } from '../../core/models/holding.model';

const PERCENT_MULTIPLIER = 100;
const BINANCE_WS_STREAM_URL = 'wss://stream.binance.com:9443/stream';

interface HoldingRow {
  holding: Holding;
  currentPrice: number;
  marketValue: number;
  totalCost: number;
  pnl: number;
  pnlPercent: number;
}

@Component({
  selector: 'app-holdings',
  standalone: true,
  imports: [DecimalPipe, DatePipe, HeaderComponent],
  templateUrl: './holdings.html',
  styleUrl: './holdings.css',
})
export class HoldingsComponent implements OnInit {
  private readonly holdingService = inject(HoldingService);
  private readonly transactionService = inject(TransactionService);
  private readonly marketData = inject(MarketDataStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private priceWs: WebSocket | null = null;

  readonly prices = signal<Record<string, number>>({});
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly expandedHoldingId = signal<number | null>(null);
  readonly transactions = signal<Transaction[]>([]);
  readonly loadingTx = signal(false);

  readonly formSymbol = signal('');
  readonly formType = signal<'BUY' | 'SELL'>('BUY');
  readonly formQuantity = signal('');
  readonly formPrice = signal('');
  readonly formDate = signal(new Date().toISOString().split('T')[0]);
  readonly formNote = signal('');
  readonly submitting = signal(false);

  readonly holdingRows = computed<HoldingRow[]>(() => {
    const holdings = this.holdingService.holdings();
    const priceMap = this.prices();
    return holdings.map((h) => {
      const currentPrice = priceMap[h.symbol.toLowerCase()] ?? 0;
      const marketValue = h.totalQty * currentPrice;
      const totalCost = h.totalQty * h.avgPrice;
      const pnl = marketValue - totalCost;
      const pnlPercent = totalCost > 0 ? (pnl / totalCost) * PERCENT_MULTIPLIER : 0;
      return { holding: h, currentPrice, marketValue, totalCost, pnl, pnlPercent };
    });
  });

  readonly portfolioTotal = computed(() => {
    const rows = this.holdingRows();
    const totalValue = rows.reduce((sum, r) => sum + r.marketValue, 0);
    const totalCost = rows.reduce((sum, r) => sum + r.totalCost, 0);
    const pnl = totalValue - totalCost;
    const pnlPercent = totalCost > 0 ? (pnl / totalCost) * PERCENT_MULTIPLIER : 0;
    return { totalValue, totalCost, pnl, pnlPercent };
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.disconnectPriceStream());
  }

  ngOnInit(): void {
    this.holdingService.load().subscribe((holdings) => {
      this.connectPriceStream(holdings);
    });
  }

  onRowClick(holding: Holding): void {
    if (this.expandedHoldingId() === holding.id) {
      this.expandedHoldingId.set(null);
      this.transactions.set([]);
      return;
    }
    this.expandedHoldingId.set(holding.id);
    this.loadingTx.set(true);
    this.transactionService.getByHolding(holding.id).subscribe({
      next: (txs) => { this.transactions.set(txs); this.loadingTx.set(false); },
      error: () => this.loadingTx.set(false),
    });
  }

  onGoToChart(holding: Holding, event: MouseEvent): void {
    event.stopPropagation();
    const symbolId = (holding.symbol + 'usdt').toLowerCase();
    this.marketData.selectSymbol({ id: symbolId, name: holding.symbol, pair: holding.symbol + '/USDT' });
    this.router.navigate(['/']);
  }

  onDeleteTransaction(tx: Transaction, event: MouseEvent): void {
    event.stopPropagation();
    this.transactionService.delete(tx.id).pipe(
      switchMap(() => this.holdingService.load())
    ).subscribe({
      next: (holdings) => {
        this.connectPriceStream(holdings);
        const holdingStillExists = holdings.some(h => h.id === this.expandedHoldingId());
        if (!holdingStillExists) {
          this.expandedHoldingId.set(null);
          this.transactions.set([]);
        } else {
          this.transactionService.getByHolding(this.expandedHoldingId()!).subscribe({
            next: (txs) => this.transactions.set(txs),
          });
        }
      },
      error: () => {},
    });
  }

  onSubmit(): void {
    const quantity = parseFloat(this.formQuantity());
    const price = parseFloat(this.formPrice());
    const symbol = this.formSymbol().toUpperCase();

    if (!symbol || isNaN(quantity) || isNaN(price)) return;

    // Oversell check
    if (this.formType() === 'SELL') {
      const holding = this.holdingService.holdings().find(h => h.symbol === symbol);
      if (!holding) {
        this.errorMessage.set(`You don't have any ${symbol} to sell.`);
        return;
      }
      if (quantity > holding.totalQty) {
        this.errorMessage.set(`You only have ${holding.totalQty} ${symbol} but tried to sell ${quantity}.`);
        return;
      }
    }

    this.errorMessage.set(null);
    this.submitting.set(true);

    const request: AddTransactionRequest = {
      symbol,
      type: this.formType(),
      quantity,
      price,
      date: this.formDate(),
      note: this.formNote() || undefined,
    };

    this.transactionService.add(request).pipe(
      switchMap(() => this.holdingService.load())
    ).subscribe({
      next: (holdings) => {
        this.resetForm();
        this.connectPriceStream(holdings);
        this.submitting.set(false);
        // Collapse if holding was fully sold
        const stillExists = holdings.some(h => h.id === this.expandedHoldingId());
        if (!stillExists) {
          this.expandedHoldingId.set(null);
          this.transactions.set([]);
        } else if (this.expandedHoldingId() !== null) {
          this.transactionService.getByHolding(this.expandedHoldingId()!).subscribe({
            next: (txs) => this.transactions.set(txs),
          });
        }
      },
      error: () => this.submitting.set(false),
    });
  }

  onCancel(): void {
    this.errorMessage.set(null);
    this.resetForm();
  }

  onFormInput(field: 'symbol' | 'quantity' | 'price' | 'date' | 'note', event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    const fieldMap = { symbol: this.formSymbol, quantity: this.formQuantity, price: this.formPrice, date: this.formDate, note: this.formNote };
    fieldMap[field].set(value);
    if (field === 'symbol' || field === 'quantity') this.errorMessage.set(null);
    if (field === 'symbol') {
      const currentPrice = this.prices()[value.toLowerCase()];
      if (currentPrice) this.formPrice.set(String(currentPrice));
    }
  }

  onTypeChange(event: Event): void {
    this.formType.set((event.target as HTMLSelectElement).value as 'BUY' | 'SELL');
    this.errorMessage.set(null);
  }

  private connectPriceStream(holdings: Holding[]): void {
    this.disconnectPriceStream();
    if (holdings.length === 0) { this.loading.set(false); return; }
    const streams = holdings.map((h) => `${(h.symbol + 'usdt').toLowerCase()}@miniTicker`).join('/');
    this.priceWs = new WebSocket(`${BINANCE_WS_STREAM_URL}?streams=${streams}`);
    this.priceWs.onmessage = (event: MessageEvent) => {
      const msg = JSON.parse(event.data as string) as { data: { s: string; c: string } };
      const symbol = msg.data.s.replace('USDT', '').toLowerCase();
      const price = parseFloat(msg.data.c);
      this.prices.update((prev) => ({ ...prev, [symbol]: price }));
      if (this.loading()) this.loading.set(false);
    };
    this.priceWs.onerror = () => { this.loading.set(false); };
  }

  private disconnectPriceStream(): void {
    if (this.priceWs) { this.priceWs.close(); this.priceWs = null; }
  }

  private resetForm(): void {
    this.showForm.set(false);
    this.formSymbol.set('');
    this.formType.set('BUY');
    this.formQuantity.set('');
    this.formPrice.set('');
    this.formDate.set(new Date().toISOString().split('T')[0]);
    this.formNote.set('');
  }
}