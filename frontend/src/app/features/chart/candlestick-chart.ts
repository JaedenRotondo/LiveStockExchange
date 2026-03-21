import {
  Component,
  ElementRef,
  ViewChild,
  DestroyRef,
  inject,
  effect,
  afterNextRender,
  ChangeDetectorRef,
  Injector,
} from '@angular/core';
import {
  createChart,
  CandlestickSeries,
  HistogramSeries,
  CrosshairMode,
  IChartApi,
  ISeriesApi,
} from 'lightweight-charts';
import { MarketDataStore } from '../../core/services/market-data.store';
import { OhlcvData } from '../../core/models/candlestick.model';
import { Timeframe } from '../../core/models/timeframe.model';
import { TIMEFRAMES } from '../../core/constants/chart.constants';
import {
  CHART_BG,
  GRID_COLOR,
  CROSSHAIR_COLOR,
  UP_COLOR,
  DOWN_COLOR,
  VOLUME_UP_COLOR,
  VOLUME_DOWN_COLOR,
} from '../../core/constants/chart.constants';
import { TEXT_SECONDARY, GREEN, RED } from '../../core/constants/theme.constants';

const VOLUME_SCALE_TOP = 0.8;
const VOLUME_SCALE_BOTTOM = 0;
const VOLUME_PRICE_SCALE_ID = 'volume';
const TOOLTIP_OFFSET_X = 15;
const TOOLTIP_OFFSET_Y = 15;
const TOOLTIP_WIDTH = 140;
const TOOLTIP_HEIGHT = 120;

@Component({
  selector: 'app-candlestick-chart',
  standalone: true,
  templateUrl: './candlestick-chart.html',
  styleUrl: './candlestick-chart.css',
})
export class CandlestickChartComponent {
  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('tooltip', { static: true }) tooltipEl!: ElementRef<HTMLDivElement>;

  tooltipData: OhlcvData | null = null;
  tooltipTime = '';
  tooltipColor = GREEN;

  readonly timeframes = TIMEFRAMES;

  private readonly marketData = inject(MarketDataStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly injector = inject(Injector);

  readonly selectedTimeframe = this.marketData.selectedTimeframe;

  private chart: IChartApi | null = null;
  private candleSeries: ISeriesApi<'Candlestick'> | null = null;
  private volumeSeries: ISeriesApi<'Histogram'> | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private prevLength = 0;

  constructor() {
    afterNextRender(() => {
      this.initChart();
      this.setupResize();

      effect(() => {
        const candles = this.marketData.candles();
        if (candles.length === 0) {
          this.prevLength = 0;
          return;
        }

        const isFullReload = this.prevLength === 0 || candles.length > this.prevLength + 1;

        if (isFullReload) {
          this.loadData(candles);
        } else {
          this.updateLastCandle(candles[candles.length - 1]);
        }

        this.prevLength = candles.length;
      }, { injector: this.injector });
    });

    this.destroyRef.onDestroy(() => {
      this.resizeObserver?.disconnect();
      this.chart?.remove();
    });
  }

  onTimeframeSelect(tf: Timeframe): void {
    this.marketData.selectTimeframe(tf);
  }

  private loadData(candles: OhlcvData[]): void {
    if (!this.candleSeries || !this.volumeSeries) {
      return;
    }

    this.candleSeries.setData(
      candles.map((c) => ({
        time: c.time as import('lightweight-charts').UTCTimestamp,
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      }))
    );

    this.volumeSeries.setData(
      candles.map((c) => ({
        time: c.time as import('lightweight-charts').UTCTimestamp,
        value: c.volume,
        color: c.close >= c.open ? VOLUME_UP_COLOR : VOLUME_DOWN_COLOR,
      }))
    );

    this.chart?.timeScale().fitContent();
  }

  private updateLastCandle(candle: OhlcvData): void {
    if (!this.candleSeries || !this.volumeSeries) {
      return;
    }

    this.candleSeries.update({
      time: candle.time as import('lightweight-charts').UTCTimestamp,
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
    });

    this.volumeSeries.update({
      time: candle.time as import('lightweight-charts').UTCTimestamp,
      value: candle.volume,
      color: candle.close >= candle.open ? VOLUME_UP_COLOR : VOLUME_DOWN_COLOR,
    });
  }

  private initChart(): void {
    const container = this.chartContainer.nativeElement;

    this.chart = createChart(container, {
      layout: {
        background: { color: CHART_BG },
        textColor: TEXT_SECONDARY,
      },
      grid: {
        vertLines: { color: GRID_COLOR },
        horzLines: { color: GRID_COLOR },
      },
      crosshair: {
        mode: CrosshairMode.Normal,
        vertLine: { color: CROSSHAIR_COLOR },
        horzLine: { color: CROSSHAIR_COLOR },
      },
      rightPriceScale: {
        borderColor: GRID_COLOR,
      },
      timeScale: {
        borderColor: GRID_COLOR,
        timeVisible: true,
        secondsVisible: false,
      },
      width: container.clientWidth,
      height: container.clientHeight,
    });

    this.candleSeries = this.chart.addSeries(CandlestickSeries, {
      upColor: UP_COLOR,
      downColor: DOWN_COLOR,
      borderVisible: false,
      wickUpColor: UP_COLOR,
      wickDownColor: DOWN_COLOR,
    });

    this.volumeSeries = this.chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' },
      priceScaleId: VOLUME_PRICE_SCALE_ID,
    });

    this.chart.priceScale(VOLUME_PRICE_SCALE_ID).applyOptions({
      scaleMargins: {
        top: VOLUME_SCALE_TOP,
        bottom: VOLUME_SCALE_BOTTOM,
      },
    });

    this.chart.subscribeCrosshairMove((param) => {
      const tooltip = this.tooltipEl.nativeElement;

      if (!param.point || !param.time || !this.candleSeries) {
        tooltip.classList.add('hidden');
        this.tooltipData = null;
        this.marketData.crosshairData.set(null);
        this.cdr.detectChanges();
        return;
      }

      const candleData = param.seriesData.get(this.candleSeries) as
        | { open: number; high: number; low: number; close: number }
        | undefined;
      const volumeData = param.seriesData.get(this.volumeSeries!) as
        | { value: number }
        | undefined;

      if (!candleData) {
        tooltip.classList.add('hidden');
        return;
      }

      const ohlcv: OhlcvData = {
        time: param.time as number,
        open: candleData.open,
        high: candleData.high,
        low: candleData.low,
        close: candleData.close,
        volume: volumeData?.value ?? 0,
      };

      this.tooltipData = ohlcv;
      this.tooltipColor = candleData.close >= candleData.open ? GREEN : RED;
      this.tooltipTime = new Date(ohlcv.time * 1000).toLocaleDateString();
      this.marketData.crosshairData.set(ohlcv);

      const containerRect = container.getBoundingClientRect();
      let x = param.point.x + TOOLTIP_OFFSET_X;
      let y = param.point.y + TOOLTIP_OFFSET_Y;

      if (x + TOOLTIP_WIDTH > containerRect.width) {
        x = param.point.x - TOOLTIP_WIDTH - TOOLTIP_OFFSET_X;
      }
      if (y + TOOLTIP_HEIGHT > containerRect.height) {
        y = param.point.y - TOOLTIP_HEIGHT - TOOLTIP_OFFSET_Y;
      }

      tooltip.style.left = `${x}px`;
      tooltip.style.top = `${y}px`;
      tooltip.classList.remove('hidden');
      this.cdr.detectChanges();
    });
  }

  private setupResize(): void {
    const container = this.chartContainer.nativeElement;

    this.resizeObserver = new ResizeObserver((entries) => {
      const { width, height } = entries[0].contentRect;
      this.chart?.resize(width, height);
    });

    this.resizeObserver.observe(container);
  }
}
