package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VWAP (Volume Weighted Average Price) with Bands.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - VWAP: TotalTypicalPriceVolume / TotalVolume
 * - Bands: VWAP +/- multiplier * StDev(TypicalPrice, length)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VWAPBands {
  /**
   * VWAP value.
   * ThinkScript: vwaps = TotalSum(typicalPrice * volume) / TotalSum(volume)
   */
  private double vwap;

  /**
   * Upper band (VWAP + multiplier * stdDev).
   * ThinkScript: vwap + multiplier * stdev
   */
  private double upperBand;

  /**
   * Lower band (VWAP - multiplier * stdDev).
   * ThinkScript: vwap - multiplier * stdev
   */
  private double lowerBand;

  /**
   * Standard deviation of price from VWAP.
   */
  private double standardDeviation;

  /**
   * Band multiplier used.
   */
  private double bandMultiplier;

  /**
   * Distance from price to VWAP as percentage.
   */
  private double distanceToVwapPercent;

  /**
   * Is price above VWAP.
   */
  private boolean isAboveVwap;

  /**
   * Is price within bands.
   */
  private boolean isWithinBands;

  /**
   * Position relative to bands.
   */
  private VwapPosition position;

  public enum VwapPosition {
    ABOVE_UPPER,    // Price > upper band
    NEAR_UPPER,     // Between VWAP and upper
    AT_VWAP,        // Within 0.1% of VWAP
    NEAR_LOWER,     // Between VWAP and lower
    BELOW_LOWER     // Price < lower band
  }

  /**
   * Determines position relative to VWAP and bands.
   */
  public static VwapPosition determinePosition(double price, double vwap,
                                                double upperBand, double lowerBand) {
    double tolerance = vwap * 0.001;

    if (Math.abs(price - vwap) < tolerance) {
      return VwapPosition.AT_VWAP;
    }
    if (price > upperBand) {
      return VwapPosition.ABOVE_UPPER;
    }
    if (price < lowerBand) {
      return VwapPosition.BELOW_LOWER;
    }
    if (price > vwap) {
      return VwapPosition.NEAR_UPPER;
    }
    return VwapPosition.NEAR_LOWER;
  }
}
