package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.MomentumTrendArrows;
import java.util.List;

/**
 * Momentum and Trend Arrows Calculator.
 * Combines MACD, RSI, and ADX for trend taxonomy.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def macdValue = MACD(fastLength, slowLength, macdLength);
 * def rsiValue = RSI(rsiLength);
 * def adxValue = ADX(adxLength);
 * def diPlus = DIPlus(adxLength);
 * def diMinus = DIMinus(adxLength);
 * 
 * # Trend arrows based on composite
 * def trendUp = macdValue > macdValue.avg and rsiValue > 50 and diPlus > diMinus;
 * def trendDown = macdValue < macdValue.avg and rsiValue < 50 and diPlus < diMinus;
 * ```
 */
public class MomentumTrendArrowsCalculator implements IndicatorCalculator<List<Bar>, MomentumTrendArrows> {

  private final int fastLength;
  private final int slowLength;
  private final int macdLength;
  private final int rsiLength;
  private final int adxLength;

  public MomentumTrendArrowsCalculator() {
    this(12, 26, 9, 14, 14);
  }

  /**
   * @param fastLength MACD fast EMA length (default 12)
   * @param slowLength MACD slow EMA length (default 26)
   * @param macdLength MACD signal length (default 9)
   * @param rsiLength RSI length (default 14)
   * @param adxLength ADX length (default 14)
   */
  public MomentumTrendArrowsCalculator(int fastLength, int slowLength, int macdLength,
                                         int rsiLength, int adxLength) {
    this.fastLength = fastLength;
    this.slowLength = slowLength;
    this.macdLength = macdLength;
    this.rsiLength = rsiLength;
    this.adxLength = adxLength;
  }

  @Override
  public MomentumTrendArrows calculate(List<Bar> bars) {
    if (bars == null || bars.size() < slowLength + macdLength) {
      return MomentumTrendArrows.builder()
          .macd(0.0)
          .macdSignal(0.0)
          .macdHistogram(0.0)
          .macdBullish(false)
          .rsi(50.0)
          .rsiZone(MomentumTrendArrows.RsiZone.NEUTRAL)
          .adx(0.0)
          .isStrongTrend(false)
          .diPlus(0.0)
          .diMinus(0.0)
          .diBullish(false)
          .trendDirection(MomentumTrendArrows.TrendDirection.NEUTRAL)
          .trendStrength(MomentumTrendArrows.TrendStrength.WEAK)
          .build();
    }

    // Calculate MACD
    double[] macdValues = calculateMACD(bars);
    double macd = macdValues[0];
    double macdSignal = macdValues[1];
    double macdHistogram = macd - macdSignal;

    // Calculate RSI
    double rsi = calculateRSI(bars, rsiLength);

    // Calculate ADX and DI
    double[] adxValues = calculateADX(bars, adxLength);
    double adx = adxValues[0];
    double diPlus = adxValues[1];
    double diMinus = adxValues[2];

    // Determine trend direction
    MomentumTrendArrows.TrendDirection trendDir = determineTrendDirection(
        macd, macdSignal, rsi, diPlus, diMinus, adx);

    return MomentumTrendArrows.builder()
        .macd(macd)
        .macdSignal(macdSignal)
        .macdHistogram(macdHistogram)
        .macdBullish(macd > macdSignal)
        .rsi(rsi)
        .rsiZone(MomentumTrendArrows.determineRsiZone(rsi))
        .adx(adx)
        .isStrongTrend(adx > 25)
        .diPlus(diPlus)
        .diMinus(diMinus)
        .diBullish(diPlus > diMinus)
        .trendDirection(trendDir)
        .trendStrength(MomentumTrendArrows.determineStrength(adx))
        .build();
  }

  /**
   * Calculate MACD: EMA(fast) - EMA(slow), and signal EMA of MACD.
   * ThinkScript: MACD(fastLength, slowLength, macdLength)
   */
  private double[] calculateMACD(List<Bar> bars) {
    double[] closes = bars.stream().mapToDouble(Bar::getClose).toArray();

    // Calculate fast and slow EMAs
    double[] fastEMA = calculateEMA(closes, fastLength);
    double[] slowEMA = calculateEMA(closes, slowLength);

    // MACD line = fast EMA - slow EMA
    double[] macdLine = new double[closes.length];
    for (int i = 0; i < closes.length; i++) {
      macdLine[i] = fastEMA[i] - slowEMA[i];
    }

    // Signal line = EMA of MACD
    double[] signalLine = calculateEMA(macdLine, macdLength);

    double currentMACD = macdLine[macdLine.length - 1];
    double currentSignal = signalLine[signalLine.length - 1];

    return new double[]{currentMACD, currentSignal};
  }

  /**
   * Calculate EMA.
   * ThinkScript: ExpAverage(price, length)
   */
  private double[] calculateEMA(double[] prices, int length) {
    double[] ema = new double[prices.length];
    double multiplier = 2.0 / (length + 1);

    // Initialize with SMA
    double sum = 0;
    for (int i = 0; i < length; i++) {
      sum += prices[i];
    }
    ema[length - 1] = sum / length;

    // Calculate EMA
    for (int i = length; i < prices.length; i++) {
      ema[i] = (prices[i] - ema[i - 1]) * multiplier + ema[i - 1];
    }

    // Fill initial values
    for (int i = 0; i < length - 1; i++) {
      ema[i] = ema[length - 1];
    }

    return ema;
  }

  /**
   * Calculate RSI.
   * ThinkScript: RSI(length)
   */
  private double calculateRSI(List<Bar> bars, int length) {
    double[] closes = bars.stream().mapToDouble(Bar::getClose).toArray();

    double avgGain = 0;
    double avgLoss = 0;

    // Initial averages
    for (int i = 1; i <= length; i++) {
      double change = closes[i] - closes[i - 1];
      if (change > 0) {
        avgGain += change;
      } else {
        avgLoss += Math.abs(change);
      }
    }
    avgGain /= length;
    avgLoss /= length;

    // Smoothed averages
    for (int i = length + 1; i < closes.length; i++) {
      double change = closes[i] - closes[i - 1];
      if (change > 0) {
        avgGain = (avgGain * (length - 1) + change) / length;
        avgLoss = (avgLoss * (length - 1)) / length;
      } else {
        avgGain = (avgGain * (length - 1)) / length;
        avgLoss = (avgLoss * (length - 1) + Math.abs(change)) / length;
      }
    }

    if (avgLoss == 0) {
      return 100;
    }

    double rs = avgGain / avgLoss;
    return 100 - (100 / (1 + rs));
  }

  /**
   * Calculate ADX, +DI, and -DI.
   * ThinkScript: ADX(length), DIPlus(length), DIMinus(length)
   */
  private double[] calculateADX(List<Bar> bars, int length) {
    int size = bars.size();
    double[] tr = new double[size];
    double[] plusDM = new double[size];
    double[] minusDM = new double[size];

    // Calculate TR, +DM, -DM
    for (int i = 1; i < size; i++) {
      Bar curr = bars.get(i);
      Bar prev = bars.get(i - 1);

      // True Range
      tr[i] = curr.getTrueRange(prev.getClose());

      // +DM and -DM
      double upMove = curr.getHigh() - prev.getHigh();
      double downMove = prev.getLow() - curr.getLow();

      if (upMove > downMove && upMove > 0) {
        plusDM[i] = upMove;
      } else {
        plusDM[i] = 0;
      }

      if (downMove > upMove && downMove > 0) {
        minusDM[i] = downMove;
      } else {
        minusDM[i] = 0;
      }
    }

    // Smooth TR, +DM, -DM
    double atr = calculateSmoothedAverage(tr, length);
    double smoothedPlusDM = calculateSmoothedAverage(plusDM, length);
    double smoothedMinusDM = calculateSmoothedAverage(minusDM, length);

    // Calculate +DI and -DI
    double diPlus = atr > 0 ? (smoothedPlusDM / atr) * 100 : 0;
    double diMinus = atr > 0 ? (smoothedMinusDM / atr) * 100 : 0;

    // Calculate DX and ADX
    double dx = 0;
    if (diPlus + diMinus > 0) {
      dx = Math.abs(diPlus - diMinus) / (diPlus + diMinus) * 100;
    }

    // Simplified ADX calculation (smoothed DX)
    double adx = dx; // In full implementation, would smooth over length

    return new double[]{adx, diPlus, diMinus};
  }

  private double calculateSmoothedAverage(double[] values, int length) {
    if (values.length < length) {
      return 0;
    }

    double sum = 0;
    int count = 0;
    for (int i = values.length - length; i < values.length; i++) {
      if (i >= 0) {
        sum += values[i];
        count++;
      }
    }

    return count > 0 ? sum / count : 0;
  }

  /**
   * Determine composite trend direction.
   */
  private MomentumTrendArrows.TrendDirection determineTrendDirection(
      double macd, double macdSignal, double rsi, double diPlus, double diMinus, double adx) {

    boolean macdBullish = macd > macdSignal;
    boolean diBullish = diPlus > diMinus;
    boolean strongTrend = adx > 25;

    int bullishScore = 0;
    if (macdBullish) bullishScore++;
    if (rsi > 50) bullishScore++;
    if (diBullish) bullishScore++;

    if (bullishScore >= 2) {
      return strongTrend ? MomentumTrendArrows.TrendDirection.STRONG_UP
          : MomentumTrendArrows.TrendDirection.UP;
    } else if (bullishScore <= 1) {
      return strongTrend ? MomentumTrendArrows.TrendDirection.STRONG_DOWN
          : MomentumTrendArrows.TrendDirection.DOWN;
    }

    return MomentumTrendArrows.TrendDirection.NEUTRAL;
  }

  @Override
  public int getRequiredDataPoints() {
    return Math.max(slowLength + macdLength, Math.max(rsiLength * 2, adxLength * 2));
  }
}
