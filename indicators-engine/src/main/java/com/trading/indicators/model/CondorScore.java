package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Condor Score indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - Proximity: how close price is to middle of range
 * - ADX: trend strength filter
 * - IV/HV: volatility context
 * - Composite: weighted combination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CondorScore {
  /**
   * Raw composite score (0-100, higher is better for condor).
   * ThinkScript: weighted average of components
   */
  private double score;

  /**
   * Proximity component: how close price is to middle of recent range.
   * ThinkScript: 100 - AbsValue(close - midPoint) / range * 100
   */
  private double proximityComponent;

  /**
   * ADX component: low ADX is better for condor.
   * ThinkScript: Max(0, 100 - ADX)
   */
  private double adxComponent;

  /**
   * IV/HV component: elevated IV is better for condor.
   * ThinkScript: ivHvRatio * factor
   */
  private double ivHvComponent;

  /**
   * Is the score favorable for condor strategy (score >= 60).
   */
  private boolean isFavorable;

  /**
   * Recommended strategy based on score.
   */
  private CondorRecommendation recommendation;

  public enum CondorRecommendation {
    STRONG_CONDOR,    // Score >= 75
    MODERATE_CONDOR,  // Score 60-74
    NEUTRAL,          // Score 40-59
    AVOID             // Score < 40
  }
}
