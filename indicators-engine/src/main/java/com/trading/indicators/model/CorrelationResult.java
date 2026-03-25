package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Correlation indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - Correlation: Correlation(close, spxClose, length)
 * - R-squared: correlation * correlation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationResult {
  /**
   * Pearson correlation coefficient (-1 to 1).
   * ThinkScript: Correlation(close, comparisonSymbol.close, length)
   */
  private double correlation;

  /**
   * R-squared (coefficient of determination).
   * ThinkScript: correlation * correlation
   */
  private double rSquared;

  /**
   * Correlation strength interpretation.
   */
  private CorrelationStrength strength;

  /**
   * Beta (slope) of the linear regression.
   * ThinkScript: Covariance(close, spxClose, length) / Sqr(StDev(spxClose, length))
   */
  private double beta;

  /**
   * Is correlation positive (directional with market).
   */
  private boolean isPositiveCorrelation;

  public enum CorrelationStrength {
    VERY_WEAK,    // |r| < 0.3
    WEAK,         // 0.3 <= |r| < 0.5
    MODERATE,     // 0.5 <= |r| < 0.7
    STRONG,       // 0.7 <= |r| < 0.9
    VERY_STRONG   // |r| >= 0.9
  }

  /**
   * Determines correlation strength from coefficient.
   */
  public static CorrelationStrength determineStrength(double correlation) {
    double absCorrelation = Math.abs(correlation);
    if (absCorrelation < 0.3) return CorrelationStrength.VERY_WEAK;
    if (absCorrelation < 0.5) return CorrelationStrength.WEAK;
    if (absCorrelation < 0.7) return CorrelationStrength.MODERATE;
    if (absCorrelation < 0.9) return CorrelationStrength.STRONG;
    return CorrelationStrength.VERY_STRONG;
  }
}
