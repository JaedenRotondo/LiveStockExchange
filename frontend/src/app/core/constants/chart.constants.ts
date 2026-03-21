import { Timeframe } from '../models/timeframe.model';

export const TIMEFRAMES: Timeframe[] = [
  { label: '1m', value: '1m' },
  { label: '5m', value: '5m' },
  { label: '15m', value: '15m' },
  { label: '1h', value: '1h' },
  { label: '4h', value: '4h' },
  { label: '1D', value: '1D' },
];

export const CHART_BG = '#161b22';
export const GRID_COLOR = '#1e293b';
export const CROSSHAIR_COLOR = '#8b949e';
export const UP_COLOR = '#26a69a';
export const DOWN_COLOR = '#ef5350';
export const VOLUME_UP_COLOR = 'rgba(38, 166, 154, 0.3)';
export const VOLUME_DOWN_COLOR = 'rgba(239, 83, 80, 0.3)';
