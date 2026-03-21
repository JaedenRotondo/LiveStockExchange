import { DestroyRef, Injectable, inject } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { OhlcvData } from '../models/candlestick.model';
import { SymbolInfo } from '../models/symbol.model';
const BINANCE_WS_API_URL = 'wss://ws-api.binance.com/ws-api/v3';
const BINANCE_WS_STREAM_URL = 'wss://stream.binance.com:9443/ws';
const DEFAULT_CANDLE_LIMIT = 200;

const KLINE_OPEN_TIME = 0;
const KLINE_OPEN = 1;
const KLINE_HIGH = 2;
const KLINE_LOW = 3;
const KLINE_CLOSE = 4;
const KLINE_VOLUME = 5;

type KlineArray = [number, string, string, string, string, string, ...unknown[]];

// Binance Kline event interface
interface BinanceKlineEvent {
  e: string;
  k: {
    t: number;
    o: string;
    h: string;
    l: string;
    c: string;
    v: string;
    x: boolean;
  };
}

// exchange info interface
interface BinanceExchangeInfo {
  symbols: {
    symbol: string;
    baseAsset: string;
    quoteAsset: string;
    status: string;
  }[];
}

const TIMEFRAME_MAP: Record<string, string> = {
  '1m': '1m',
  '5m': '5m',
  '15m': '15m',
  '1h': '1h',
  '4h': '4h',
  '1D': '1d',
};

const MS_TO_SECONDS = 1000;
const EXCHANGE_INFO_CACHE_KEY = 'binance_exchange_info';
const EXCHANGE_INFO_TTL_MS = 24 * 60 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class BinanceApiClient {
  private readonly destroyRef = inject(DestroyRef);
  private ws: WebSocket | null = null;
  private readonly candleSubject = new Subject<{ candle: OhlcvData; isClosed: boolean }>();

  readonly candleUpdates$: Observable<{ candle: OhlcvData; isClosed: boolean }> =
    this.candleSubject.asObservable();

  readonly exchangeInfo$ = this.loadExchangeInfo();

  constructor() {
    this.destroyRef.onDestroy(() => this.disconnect());
  }

  private loadExchangeInfo(): Observable<SymbolInfo[]> {
    const cached = localStorage.getItem(EXCHANGE_INFO_CACHE_KEY);
    if (cached) {
      const { data, timestamp } = JSON.parse(cached) as { data: SymbolInfo[]; timestamp: number };
      if (Date.now() - timestamp < EXCHANGE_INFO_TTL_MS) {
        return of(data);
      }
    }

    return this.fetchExchangeInfo();
  }

  private fetchExchangeInfo(): Observable<SymbolInfo[]> {
    return new Observable<SymbolInfo[]>((observer) => {
      const apiWs = new WebSocket(BINANCE_WS_API_URL);

      apiWs.onopen = () => {
        apiWs.send(
          JSON.stringify({
            id: crypto.randomUUID(),
            method: 'exchangeInfo',
            params: {},
          }),
        );
      };

      apiWs.onmessage = (event: MessageEvent) => {
        const response = JSON.parse(event.data as string) as {
          id: string;
          status: number;
          result: BinanceExchangeInfo;
        };
        if (response.status === 200) {
          const symbols = response.result.symbols
            .filter((s) => s.status === 'TRADING' && s.quoteAsset === 'USDT')
            .map((s) => ({
              id: s.symbol.toLowerCase(),
              name: s.baseAsset,
              pair: s.baseAsset + '/' + s.quoteAsset,
            }));
          localStorage.setItem(
            EXCHANGE_INFO_CACHE_KEY,
            JSON.stringify({ data: symbols, timestamp: Date.now() }),
          );
          observer.next(symbols);
          observer.complete();
        } else {
          observer.error(new Error(`Binance WS API error: ${response.status}`));
        }

        apiWs.close();
      };

      apiWs.onerror = () => {
        observer.error(new Error('Binance WS API connection failed'));
        apiWs.close();
      };

      return () => apiWs.close();
    });
  }

  fetchCandles(
    symbol: string,
    interval: string,
    limit: number = DEFAULT_CANDLE_LIMIT,
  ): Observable<OhlcvData[]> {
    const binanceInterval = TIMEFRAME_MAP[interval] ?? interval;

    return new Observable<OhlcvData[]>((observer) => {
      const id = crypto.randomUUID();
      const apiWs = new WebSocket(BINANCE_WS_API_URL);

      apiWs.onopen = () => {
        apiWs.send(
          JSON.stringify({
            id,
            method: 'klines',
            params: { symbol: symbol.toUpperCase(), interval: binanceInterval, limit },
          }),
        );
      };

      apiWs.onmessage = (event: MessageEvent) => {
        const response = JSON.parse(event.data as string) as {
          id: string;
          status: number;
          result: KlineArray[];
        };
        if (response.id !== id) return;

        if (response.status === 200) {
          observer.next(
            response.result.map((k) => ({
              time: Math.floor(k[KLINE_OPEN_TIME] / MS_TO_SECONDS),
              open: parseFloat(k[KLINE_OPEN] as string),
              high: parseFloat(k[KLINE_HIGH] as string),
              low: parseFloat(k[KLINE_LOW] as string),
              close: parseFloat(k[KLINE_CLOSE] as string),
              volume: parseFloat(k[KLINE_VOLUME] as string),
            })),
          );
          observer.complete();
        } else {
          observer.error(new Error(`Binance WS API error: ${response.status}`));
        }

        apiWs.close();
      };

      apiWs.onerror = () => {
        observer.error(new Error('Binance WS API connection failed'));
        apiWs.close();
      };

      return () => apiWs.close();
    });
  }

  connect(symbol: string, interval: string): void {
    this.disconnect();

    const binanceInterval = TIMEFRAME_MAP[interval] ?? interval;
    const stream = `${symbol.toLowerCase()}@kline_${binanceInterval}`;
    this.ws = new WebSocket(`${BINANCE_WS_STREAM_URL}/${stream}`);

    this.ws.onmessage = (event: MessageEvent) => {
      const data: BinanceKlineEvent = JSON.parse(event.data as string);
      const k = data.k;

      this.candleSubject.next({
        candle: {
          time: Math.floor(k.t / MS_TO_SECONDS),
          open: parseFloat(k.o),
          high: parseFloat(k.h),
          low: parseFloat(k.l),
          close: parseFloat(k.c),
          volume: parseFloat(k.v),
        },
        isClosed: k.x,
      });
    };
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}
