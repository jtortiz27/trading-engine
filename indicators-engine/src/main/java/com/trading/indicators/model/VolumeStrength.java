package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Volume Strength indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - VolumeRatio (VR): current volume / avg volume at same time
 * - RTH projection: current volume / avg volume of first N bars * remaining bars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeStrength {
  /**
   * Volume Ratio: current volume / average volume at same time of day.
   * ThinkScript: volume / Average(volume, length) for same bar position
   */
  private double volumeRatio;

  /**
   * Projected RTH (Regular Trading Hours) volume based on current pace.
   * ThinkScript: volume * (totalRTHBars / currentBarOfDay)
   */
  private double projectedRthVolume;

  /**
   * Percentage of average volume (VR * 100).
   */
  private double volumePercentOfAvg;

  /**
   * Is volume considered "strong" (VR > threshold).
   */
  private boolean isStrongVolume;
}
