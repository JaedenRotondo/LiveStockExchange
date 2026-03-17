import { SymbolInfo } from '../models/symbol.model';
import { OhlcvData } from '../models/candlestick.model';

export const SYMBOLS: SymbolInfo[] = [
  { id: 'btcusdt', name: 'Bitcoin', pair: 'BTC/USDT' },
  { id: 'ethusdt', name: 'Ethereum', pair: 'ETH/USDT' },
  { id: 'solusdt', name: 'Solana', pair: 'SOL/USDT' },
  { id: 'bnbusdt', name: 'BNB', pair: 'BNB/USDT' },
  { id: 'xrpusdt', name: 'XRP', pair: 'XRP/USDT' },
  { id: 'adausdt', name: 'Cardano', pair: 'ADA/USDT' },
  { id: 'dogeusdt', name: 'Dogecoin', pair: 'DOGE/USDT' },
  { id: 'dotusdt', name: 'Polkadot', pair: 'DOT/USDT' },
  { id: 'avaxusdt', name: 'Avalanche', pair: 'AVAX/USDT' },
  { id: 'maticusdt', name: 'Polygon', pair: 'MATIC/USDT' },
  { id: 'linkusdt', name: 'Chainlink', pair: 'LINK/USDT' },
  { id: 'atomusdt', name: 'Cosmos', pair: 'ATOM/USDT' },
];

const BASE_PRICES: Record<string, number> = {
  btcusdt: 42000,
  ethusdt: 3200,
  solusdt: 148,
  bnbusdt: 580,
  xrpusdt: 0.62,
  adausdt: 0.45,
  dogeusdt: 0.12,
  dotusdt: 7.8,
  avaxusdt: 35,
  maticusdt: 0.85,
  linkusdt: 15,
  atomusdt: 9.5,
};

const SEED_MULTIPLIER = 1103515245;
const SEED_INCREMENT = 12345;
const SEED_MODULUS = 2147483648;

function seededRandom(seed: number): { value: number; nextSeed: number } {
  const nextSeed = (seed * SEED_MULTIPLIER + SEED_INCREMENT) % SEED_MODULUS;
  return { value: nextSeed / SEED_MODULUS, nextSeed };
}

const DAY_IN_SECONDS = 86400;
const DEFAULT_CANDLE_COUNT = 200;
const VOLATILITY_FACTOR = 0.02;
const WICK_FACTOR = 0.01;
const BASE_VOLUME_MIN = 100;
const BASE_VOLUME_RANGE = 900;

export function generateDummyCandles(
  symbolId: string = 'btcusdt',
  count: number = DEFAULT_CANDLE_COUNT
): OhlcvData[] {
  const basePrice = BASE_PRICES[symbolId] ?? 100;
  const candles: OhlcvData[] = [];
  let currentPrice = basePrice;
  let seed = symbolId.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);

  const startTime = Math.floor(Date.now() / 1000) - count * DAY_IN_SECONDS;

  for (let i = 0; i < count; i++) {
    const r1 = seededRandom(seed);
    seed = r1.nextSeed;
    const r2 = seededRandom(seed);
    seed = r2.nextSeed;
    const r3 = seededRandom(seed);
    seed = r3.nextSeed;

    const change = (r1.value - 0.5) * VOLATILITY_FACTOR * currentPrice;
    const open = currentPrice;
    const close = open + change;
    const high = Math.max(open, close) + r2.value * WICK_FACTOR * currentPrice;
    const low = Math.min(open, close) - r3.value * WICK_FACTOR * currentPrice;
    const volume = BASE_VOLUME_MIN + r2.value * BASE_VOLUME_RANGE;

    candles.push({
      time: startTime + i * DAY_IN_SECONDS,
      open: parseFloat(open.toFixed(2)),
      high: parseFloat(high.toFixed(2)),
      low: parseFloat(low.toFixed(2)),
      close: parseFloat(close.toFixed(2)),
      volume: parseFloat(volume.toFixed(2)),
    });

    currentPrice = close;
  }

  return candles;
}
