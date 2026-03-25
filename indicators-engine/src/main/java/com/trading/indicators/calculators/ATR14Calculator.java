package com.trading.indicators.calculators;

import com.trading.indicators.model.ATR14;
import com.trading.indicators.model.Bar;
import java.util.List;

/**
 * ATR (Average True Range) Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def tr = TrueRange(high, close, low);
 * def atr = Average(tr, length);
 * ```
 */
public class ATR14Calculator implements IndicatorCalculator<List<Bar>, ATR14> {

  private final int length;

  public ATR14Calculator() {
    this(14);
  }

  /**
   * @param length Lookback period for ATR (default 14)
   */
  public ATR14Calculator(int length) {
    this.length = length;
  }

  @Override
  public ATR14 calculate(List<Bar> bars) {
    if (bars == null || bars.size() < length + 1) {
      return ATR14.builder()
          .atr(0.0)
          .atrPercent(0.0)
          .trueRange(0.0)
          .volatilityLevel(ATR14.VolatilityLevel.VERY_LOW)
          .build();
    }

    // Calculate True Range for each bar
    double[] trueRanges = new double[bars.size()];
    trueRanges[0] = bars.get(0).getHigh() - bars.get(0).getLow();

    for (int i = 1; i < bars.size(); i++) {
      Bar curr = bars.get(i);
      Bar prev = bars.get(i - 1);
      trueRanges[i] = curr.getTrueRange(prev.getClose());
    }

    // Calculate ATR using Wilder's smoothing
    double atr = calculateWildersATR(trueRanges, length);

    // Current True Range
    double currentTR = trueRanges[trueRanges.length - 1];

    // Current close for ATR%
    double currentClose = bars.get(bars.size() - 1).getClose();
    double atrPercent = currentClose > 0 ? (atr / currentClose) * 100.0 : 0.0;

    return ATR14.builder()
        .atr(atr)
        .atrPercent(atrPercent)
        .trueRange(currentTR)
        .volatilityLevel(ATR14.determineLevel(atrPercent))
        .build();
  }

  /**
   * Calculate ATR using Wilder's smoothing method.
   * ThinkScript: Average(TrueRange, length) uses Wilder's smoothing
   */
  private double calculateWildersATR(double[] trueRanges, int length) {
    int size = trueRanges.length;
    if (size < length) {
      return 0.0;
    }

    // Initial SMA
    double atr = 0.0;
    for (int i = size - length; i < size; i++) {
      atr += trueRanges[i];
    }
    atr /= length;

    // Apply Wilder's smoothing for historical values
    // ATR[i] = (ATR[i-1] * (length-1) + TR[i]) / length
    for (int i = size - length; i < size; i++) {
      atr = (atr * (length - 1) + trueRanges[i]) / length;
    }

    return atr;
  }

  @Override
  public int getRequiredDataPoints() {
    return length + 1;
  }
}
