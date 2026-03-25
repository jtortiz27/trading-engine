package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ATR (Average True Range) indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - ATR: Average(TrueRange, length)
 * - ATR%: ATR / close * 100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ATR14 {
  /**
   * ATR value (absolute).
   * ThinkScript: Average(TrueRange, 14)
   */
  private double atr;

  /**
   * ATR as percentage of price.
   * ThinkScript: ATR / close * 100
   */
  private double atrPercent;

  /**
   * Current True Range.
   * ThinkScript: TrueRange = Max(high - low, AbsValue(high - close[1]), AbsValue(low - close[1]))
   */
  private double trueRange;

  /**
   * Volatility classification based on ATR%.
   */
  private VolatilityLevel volatilityLevel;

  public enum VolatilityLevel {
    VERY_LOW,   // ATR% < 0.5
    LOW,        // 0.5 - 1.0
    MODERATE,   // 1.0 - 2.0
    HIGH,       // 2.0 - 4.0
    VERY_HIGH   // ATR% > 4.0
  }

  /**
   * Determines volatility level from ATR%.
   */
  public static VolatilityLevel determineLevel(double atrPercent) {
    if (atrPercent < 0.5) return VolatilityLevel.VERY_LOW;
    if (atrPercent < 1.0) return VolatilityLevel.LOW;
    if (atrPercent < 2.0) return VolatilityLevel.MODERATE;
    if (atrPercent < 4.0) return VolatilityLevel.HIGH;
    return VolatilityLevel.VERY_HIGH;
  }
}
