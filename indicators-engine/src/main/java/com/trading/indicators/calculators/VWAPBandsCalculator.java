package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.VWAPBands;
import java.util.List;

/**
 * VWAP (Volume Weighted Average Price) with Bands Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def typicalPrice = (high + low + close) / 3;
 * def cumulativeTypicalPriceVolume = TotalSum(typicalPrice * volume);
 * def cumulativeVolume = TotalSum(volume);
 * def vwap = cumulativeTypicalPriceVolume / cumulativeVolume;
 * def deviation = StDev(typicalPrice, length);
 * plot upperBand = vwap + multiplier * deviation;
 * plot lowerBand = vwap - multiplier * deviation;
 * ```
 */
public class VWAPBandsCalculator implements IndicatorCalculator<List<Bar>, VWAPBands> {

  private final double bandMultiplier;
  private final int length;

  public VWAPBandsCalculator() {
    this(1.5, Integer.MAX_VALUE); // Default 1.5 std dev, cumulative VWAP
  }

  /**
   * @param bandMultiplier Standard deviation multiplier for bands (default 1.5)
   * @param length Lookback period for standard deviation (MAX_VALUE for cumulative VWAP)
   */
  public VWAPBandsCalculator(double bandMultiplier, int length) {
    this.bandMultiplier = bandMultiplier;
    this.length = length;
  }

  @Override
  public VWAPBands calculate(List<Bar> bars) {
    if (bars == null || bars.isEmpty()) {
      return VWAPBands.builder()
          .vwap(0.0)
          .upperBand(0.0)
          .lowerBand(0.0)
          .standardDeviation(0.0)
          .bandMultiplier(bandMultiplier)
          .distanceToVwapPercent(0.0)
          .isAboveVwap(false)
          .isWithinBands(false)
          .position(VWAPBands.VwapPosition.AT_VWAP)
          .build();
    }

    // Calculate VWAP (cumulative)
    double totalTypicalPriceVolume = 0.0;
    double totalVolume = 0.0;

    int startIdx = Math.max(0, bars.size() - length);
    for (int i = startIdx; i < bars.size(); i++) {
      Bar bar = bars.get(i);
      double typicalPrice = bar.getTypicalPrice();
      double volume = bar.getVolume();

      totalTypicalPriceVolume += typicalPrice * volume;
      totalVolume += volume;
    }

    double vwap = totalVolume > 0 ? totalTypicalPriceVolume / totalVolume : 0.0;

    // Calculate standard deviation
    double sumSquaredDiff = 0.0;
    double sumVolume = 0.0;

    for (int i = startIdx; i < bars.size(); i++) {
      Bar bar = bars.get(i);
      double typicalPrice = bar.getTypicalPrice();
      double volume = bar.getVolume();
      double diff = typicalPrice - vwap;

      sumSquaredDiff += diff * diff * volume;
      sumVolume += volume;
    }

    double variance = sumVolume > 0 ? sumSquaredDiff / sumVolume : 0.0;
    double stdDev = Math.sqrt(variance);

    // Calculate bands
    double upperBand = vwap + (bandMultiplier * stdDev);
    double lowerBand = vwap - (bandMultiplier * stdDev);

    // Current price for position
    double currentPrice = bars.get(bars.size() - 1).getClose();
    double distanceToVwapPercent = vwap > 0 ? ((currentPrice - vwap) / vwap) * 100.0 : 0.0;

    return VWAPBands.builder()
        .vwap(vwap)
        .upperBand(upperBand)
        .lowerBand(lowerBand)
        .standardDeviation(stdDev)
        .bandMultiplier(bandMultiplier)
        .distanceToVwapPercent(distanceToVwapPercent)
        .isAboveVwap(currentPrice > vwap)
        .isWithinBands(currentPrice >= lowerBand && currentPrice <= upperBand)
        .position(VWAPBands.determinePosition(currentPrice, vwap, upperBand, lowerBand))
        .build();
  }

  @Override
  public int getRequiredDataPoints() {
    return 1;
  }
}
