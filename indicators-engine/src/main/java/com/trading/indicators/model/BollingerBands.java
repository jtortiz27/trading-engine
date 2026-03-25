package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bollinger Bands indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - Middle: Average(close, length)
 * - Upper: Middle + deviation * StDev(close, length)
 * - Lower: Middle - deviation * StDev(close, length)
 * - BandWidth: (Upper - Lower) / Middle
 * - %B: (close - Lower) / (Upper - Lower)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BollingerBands {
  /**
   * Middle band (SMA of closes).
   * ThinkScript: Average(close, length)
   */
  private double middle;

  /**
   * Upper band.
   * ThinkScript: middle + deviation * StDev(close, length)
   */
  private double upper;

  /**
   * Lower band.
   * ThinkScript: middle - deviation * StDev(close, length)
   */
  private double lower;

  /**
   * Standard deviation used in calculation.
   */
  private double standardDeviation;

  /**
   * Band width as percentage of middle band.
   * ThinkScript: (upper - lower) / middle
   */
  private double bandwidthPercent;

  /**
   * %B indicator: position within bands (0 = lower, 1 = upper).
   * ThinkScript: (close - lower) / (upper - lower)
   */
  private double percentB;

  /**
   * Is price above upper band (potential overbought).
   */
  private boolean isAboveUpper;

  /**
   * Is price below lower band (potential oversold).
   */
  private boolean isBelowLower;

  /**
   * Squeeze condition: bands are narrow (bandwidth < threshold).
   */
  private boolean isSqueeze;

  /**
   * Bandwidth classification.
   */
  private BandwidthLevel bandwidthLevel;

  public enum BandwidthLevel {
    SQUEEZE,    // < 10%
    NARROW,     // 10-15%
    NORMAL,     // 15-25%
    WIDE,       // 25-40%
    VERY_WIDE   // > 40%
  }

  /**
   * Determines bandwidth level.
   */
  public static BandwidthLevel determineBandwidthLevel(double bandwidthPercent) {
    if (bandwidthPercent < 10) return BandwidthLevel.SQUEEZE;
    if (bandwidthPercent < 15) return BandwidthLevel.NARROW;
    if (bandwidthPercent < 25) return BandwidthLevel.NORMAL;
    if (bandwidthPercent < 40) return BandwidthLevel.WIDE;
    return BandwidthLevel.VERY_WIDE;
  }
}
