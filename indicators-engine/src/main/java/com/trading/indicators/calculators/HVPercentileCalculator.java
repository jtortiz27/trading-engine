package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.HVPercentile;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical Volatility Percentile Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def hv = historical_volatility(hvLength);
 * def hvPercentile = fold i = 0 to rankLen with count = 0 do if hv[i] < hv then count + 1 else count;
 * def hvPercentileValue = hvPercentile / rankLen * 100;
 * ```
 */
public class HVPercentileCalculator implements IndicatorCalculator<List<Bar>, HVPercentile> {

  private final int hvLength;      // Period for HV calculation (e.g., 30)
  private final int rankLength;    // Period for percentile/rank (e.g., 252)
  private final double highThreshold;
  private final double lowThreshold;

  public HVPercentileCalculator() {
    this(30, 252, 80.0, 20.0);
  }

  /**
   * @param hvLength Period for HV calculation
   * @param rankLength Period for percentile/rank calculation
   * @param highThreshold Percentile threshold for "elevated" (default 80)
   * @param lowThreshold Percentile threshold for "low" (default 20)
   */
  public HVPercentileCalculator(int hvLength, int rankLength, double highThreshold, double lowThreshold) {
    this.hvLength = hvLength;
    this.rankLength = rankLength;
    this.highThreshold = highThreshold;
    this.lowThreshold = lowThreshold;
  }

  @Override
  public HVPercentile calculate(List<Bar> bars) {
    if (bars == null || bars.size() < hvLength + rankLength) {
      return HVPercentile.builder()
          .historicalVolatility(0.0)
          .hvPercentile(50.0)
          .hvRank(50.0)
          .isHVElevated(false)
          .isHVLow(false)
          .build();
    }

    // Calculate current HV
    double currentHV = calculateHV(bars, bars.size() - 1, hvLength);

    // Calculate historical HV series for percentile/rank
    List<Double> hvSeries = new ArrayList<>();
    for (int i = bars.size() - rankLength - hvLength; i < bars.size(); i++) {
      if (i >= hvLength) {
        hvSeries.add(calculateHV(bars, i, hvLength));
      }
    }

    // Calculate percentile and rank
    double hvPercentile = calculatePercentile(hvSeries, currentHV);
    double hvRank = calculateRank(hvSeries, currentHV);

    return HVPercentile.builder()
        .historicalVolatility(currentHV)
        .hvPercentile(hvPercentile)
        .hvRank(hvRank)
        .isHVElevated(hvPercentile >= highThreshold)
        .isHVLow(hvPercentile <= lowThreshold)
        .build();
  }

  /**
   * Calculate historical volatility at a specific index.
   * ThinkScript: historical_volatility(length) = StDev(log(close/close[1])) * Sqrt(252) * 100
   */
  private double calculateHV(List<Bar> bars, int endIndex, int length) {
    if (endIndex < length || endIndex >= bars.size()) {
      return 0.0;
    }

    double sumLogReturns = 0.0;
    double sumSquaredLogReturns = 0.0;

    for (int i = endIndex - length + 1; i <= endIndex; i++) {
      double prevClose = bars.get(i - 1).getClose();
      double currClose = bars.get(i).getClose();

      if (prevClose > 0) {
        double logReturn = Math.log(currClose / prevClose);
        sumLogReturns += logReturn;
        sumSquaredLogReturns += logReturn * logReturn;
      }
    }

    double meanLogReturn = sumLogReturns / length;
    double variance = (sumSquaredLogReturns / length) - (meanLogReturn * meanLogReturn);
    double stdDev = Math.sqrt(Math.max(0, variance));

    // Annualize and convert to percentage
    return stdDev * Math.sqrt(252) * 100;
  }

  /**
   * Calculate percentile: percentage of values below current.
   * ThinkScript: fold i = 0 to rankLen with count = 0 do if hv[i] < hv then count + 1 else count
   */
  private double calculatePercentile(List<Double> series, double currentValue) {
    if (series.isEmpty()) {
      return 50.0;
    }

    int belowCount = 0;
    for (double value : series) {
      if (value < currentValue) {
        belowCount++;
      }
    }

    return (belowCount / (double) series.size()) * 100.0;
  }

  /**
   * Calculate rank: (current - min) / (max - min) * 100.
   * ThinkScript: (hv - lowest(hv, rankLen)) / (highest(hv, rankLen) - lowest(hv, rankLen)) * 100
   */
  private double calculateRank(List<Double> series, double currentValue) {
    if (series.isEmpty()) {
      return 50.0;
    }

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (double value : series) {
      min = Math.min(min, value);
      max = Math.max(max, value);
    }

    if (max == min) {
      return 50.0;
    }

    return ((currentValue - min) / (max - min)) * 100.0;
  }

  @Override
  public int getRequiredDataPoints() {
    return hvLength + rankLength;
  }
}
