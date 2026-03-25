package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.BollingerBands;
import java.util.List;

/**
 * Bollinger Bands Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def middle = Average(close, length);
 * def deviation = StDev(close, length);
 * plot upper = middle + numDev * deviation;
 * plot lower = middle - numDev * deviation;
 * def bandwidth = (upper - lower) / middle;
 * def percentB = (close - lower) / (upper - lower);
 * ```
 */
public class BollingerBandsCalculator implements IndicatorCalculator<List<Bar>, BollingerBands> {

  private final int length;
  private final double numDeviations;
  private final double squeezeThreshold;

  public BollingerBandsCalculator() {
    this(20, 2.0, 0.10);
  }

  /**
   * @param length SMA period (default 20)
   * @param numDeviations Number of standard deviations (default 2.0)
   * @param squeezeThreshold Bandwidth threshold for squeeze detection (default 0.10 = 10%)
   */
  public BollingerBandsCalculator(int length, double numDeviations, double squeezeThreshold) {
    this.length = length;
    this.numDeviations = numDeviations;
    this.squeezeThreshold = squeezeThreshold;
  }

  @Override
  public BollingerBands calculate(List<Bar> bars) {
    if (bars == null || bars.size() < length) {
      return BollingerBands.builder()
          .middle(0.0)
          .upper(0.0)
          .lower(0.0)
          .standardDeviation(0.0)
          .bandwidthPercent(0.0)
          .percentB(0.5)
          .isAboveUpper(false)
          .isBelowLower(false)
          .isSqueeze(false)
          .bandwidthLevel(BollingerBands.BandwidthLevel.NORMAL)
          .build();
    }

    // Get closes
    double[] closes = new double[length];
    int startIdx = bars.size() - length;
    for (int i = 0; i < length; i++) {
      closes[i] = bars.get(startIdx + i).getClose();
    }

    // Calculate SMA (middle band)
    double sum = 0.0;
    for (double close : closes) {
      sum += close;
    }
    double middle = sum / length;

    // Calculate standard deviation
    double sumSquaredDiff = 0.0;
    for (double close : closes) {
      double diff = close - middle;
      sumSquaredDiff += diff * diff;
    }
    double variance = sumSquaredDiff / length;
    double stdDev = Math.sqrt(variance);

    // Calculate upper and lower bands
    double upper = middle + (numDeviations * stdDev);
    double lower = middle - (numDeviations * stdDev);

    // Current close for position calculations
    double currentClose = bars.get(bars.size() - 1).getClose();

    // Calculate bandwidth percentage
    double bandwidthPercent = middle > 0 ? ((upper - lower) / middle) * 100.0 : 0.0;

    // Calculate %B
    double bandRange = upper - lower;
    double percentB = bandRange > 0 ? (currentClose - lower) / bandRange : 0.5;

    // Clamp %B to [0, 1]
    percentB = Math.max(0.0, Math.min(1.0, percentB));

    return BollingerBands.builder()
        .middle(middle)
        .upper(upper)
        .lower(lower)
        .standardDeviation(stdDev)
        .bandwidthPercent(bandwidthPercent)
        .percentB(percentB)
        .isAboveUpper(currentClose > upper)
        .isBelowLower(currentClose < lower)
        .isSqueeze(bandwidthPercent < squeezeThreshold * 100)
        .bandwidthLevel(BollingerBands.determineBandwidthLevel(bandwidthPercent))
        .build();
  }

  @Override
  public int getRequiredDataPoints() {
    return length;
  }
}
