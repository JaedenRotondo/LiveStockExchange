import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Subscription, catchError, of } from 'rxjs';
import { TIMEFRAMES } from '../constants/chart.constants';
import { OhlcvData } from '../models/candlestick.model';
import { SymbolInfo, SymbolQuote } from '../models/symbol.model';
import { Timeframe } from '../models/timeframe.model';
import { BinanceApiClient } from './binance-api-client';

const DEFAULT_TIMEFRAME_INDEX = 0;
const PERCENT_MULTIPLIER = 100;

// Default hardcoded symbol
const BTCUSDT_SYMBOL: SymbolInfo = {
  id: 'btcusdt',
  name: 'BTC',
  pair: 'BTC/USDT',
};

@Injectable({ providedIn: 'root' })
export class MarketDataStore {
  private readonly binance = inject(BinanceApiClient);
  private readonly destroyRef = inject(DestroyRef);
  private subscription: Subscription | null = null;

  readonly selectedSymbol = signal<SymbolInfo>(BTCUSDT_SYMBOL); // Default to BTC/USDT
  readonly selectedTimeframe = signal<Timeframe>(TIMEFRAMES[DEFAULT_TIMEFRAME_INDEX]);
  readonly candles = signal<OhlcvData[]>([]);
  readonly crosshairData = signal<OhlcvData | null>(null);
  readonly exchangeInfo$ = this.binance.exchangeInfo$; // Cache the exchange info to avoid redundant API calls (static info)

  readonly currentQuote = computed<SymbolQuote>(() => {
    const data = this.candles();
    if (data.length === 0) {
      return { price: 0, change: 0, changePercent: 0 };
    }

    const lastCandle = data[data.length - 1];
    const firstCandle = data[0];
    const change = lastCandle.close - firstCandle.open;
    const changePercent = (change / firstCandle.open) * PERCENT_MULTIPLIER;

    return {
      price: lastCandle.close,
      change,
      changePercent,
    };
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.subscription?.unsubscribe());
    this.startStream();
  }

  selectSymbol(symbol: SymbolInfo): void {
    this.selectedSymbol.set(symbol);
    this.crosshairData.set(null);
    this.candles.set([]);
    this.startStream();
  }

  selectTimeframe(timeframe: Timeframe): void {
    this.selectedTimeframe.set(timeframe);
    this.crosshairData.set(null);
    this.candles.set([]);
    this.startStream();
  }

  private startStream(): void {
    this.subscription?.unsubscribe(); // Unsubscribe from any existing stream before starting a new one

    const symbol = this.selectedSymbol().id;
    const interval = this.selectedTimeframe().value;

    this.binance.connect(symbol, interval); // Open a persistent WS connection for live updates

    this.binance
      .fetchCandles(symbol, interval) // Fetch historical data for the selected symbol and timeframe (simultaneously with opening the live stream)
      .pipe(catchError(() => of([] as OhlcvData[])))
      .subscribe((history) => {
        if (history.length > 0) {
          this.candles.set(history); // Set historical data first, then live updates will append to it as they arrive
        }

        this.subscription = this.binance.candleUpdates$.subscribe(({ candle, isClosed }) => {
          const current = this.candles();
          if (current.length === 0) {
            // If we haven't received historical data yet, we can't determine how to merge the incoming candle, so we just ignore it until we have the full history. This prevents issues with out-of-order candles or missing data points.
            return; // skip if we haven't received historical data yet
          }

          const lastCandle = current[current.length - 1];

          if (candle.time === lastCandle.time) {
            this.candles.set([...current.slice(0, -1), candle]);
          } else if (isClosed || candle.time > lastCandle.time) {
            this.candles.set([...current, candle]);
          }
        });
      });
  }
}
