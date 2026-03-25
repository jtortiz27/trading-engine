package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drift Efficiency indicator.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - Drift: (close - open) / open
 * - Range: high - low
 * - Efficiency: drift / range (normalized by range)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftEfficiency {
  /**
   * Drift: (close - open) / open as percentage.
   * ThinkScript: (close - open) / open * 100
   */
  private double drift;

  /**
   * Range of the bar (high - low).
   * ThinkScript: high - low
   */
  private double range;

  /**
   * Drift Efficiency: drift / range.
   * ThinkScript: AbsValue(drift) / range * 100
   * Higher values mean price moved efficiently with less noise.
   */
  private double efficiency;

  /**
   * Is drift positive (close > open).
   */
  private boolean isPositiveDrift;

  /**
   * Is efficiency high (> 50%).
   */
  private boolean isHighEfficiency;

  /**
   * Efficiency classification.
   */
  private EfficiencyLevel efficiencyLevel;

  public enum EfficiencyLevel {
    VERY_LOW,   // < 25%
    LOW,        // 25-40%
    MODERATE,   // 40-60%
    HIGH,       // 60-80%
    VERY_HIGH   // > 80%
  }

  /**
   * Determines efficiency level.
   */
  public static EfficiencyLevel determineLevel(double efficiency) {
    double absEfficiency = Math.abs(efficiency);
    if (absEfficiency < 25) return EfficiencyLevel.VERY_LOW;
    if (absEfficiency < 40) return EfficiencyLevel.LOW;
    if (absEfficiency < 60) return EfficiencyLevel.MODERATE;
    if (absEfficiency < 80) return EfficiencyLevel.HIGH;
    return EfficiencyLevel.VERY_HIGH;
  }
}
