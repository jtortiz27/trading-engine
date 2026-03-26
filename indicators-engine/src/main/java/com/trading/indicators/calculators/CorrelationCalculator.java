package com.trading.indicators.calculators;

import com.trading.indicators.model.CorrelationResult;
import java.util.List;

/**
 * Correlation Calculator for two price series.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def correlation = Correlation(close, close(symbol = "SPX"), length);
 * def rSquared = correlation * correlation;
 * def beta = Covariance(close, spxClose, length) / Sqr(StDev(spxClose, length));
 * ```
 */
public class CorrelationCalculator implements IndicatorCalculator<CorrelationCalculator.CorrelationInput, CorrelationResult> {

  private final int length;

  public CorrelationCalculator() {
    this(30);
  }

  /**
   * @param length Lookback period for correlation (default 30)
   */
  public CorrelationCalculator(int length) {
    this.length = length;
  }

  @Override
  public CorrelationResult calculate(CorrelationInput input) {
    List<Double> primaryPrices = input.getPrimaryPrices();
    List<Double> comparisonPrices = input.getComparisonPrices();

    if (primaryPrices == null || comparisonPrices == null
        || primaryPrices.size() < length || comparisonPrices.size() < length) {
      return CorrelationResult.builder()
          .correlation(0.0)
          .rSquared(0.0)
          .strength(CorrelationResult.CorrelationStrength.VERY_WEAK)
          .beta(1.0)
          .isPositiveCorrelation(false)
          .build();
    }

    // Calculate means
    double meanX = calculateMean(primaryPrices, length);
    double meanY = calculateMean(comparisonPrices, length);

    // Calculate covariance and variances
    double covariance = 0.0;
    double varianceX = 0.0;
    double varianceY = 0.0;

    int startIdx = primaryPrices.size() - length;
    int startIdxComp = comparisonPrices.size() - length;

    for (int i = 0; i < length; i++) {
      double x = primaryPrices.get(startIdx + i);
      double y = comparisonPrices.get(startIdxComp + i);

      double diffX = x - meanX;
      double diffY = y - meanY;

      covariance += diffX * diffY;
      varianceX += diffX * diffX;
      varianceY += diffY * diffY;
    }

    covariance /= length;
    varianceX /= length;
    varianceY /= length;

    double stdDevX = Math.sqrt(varianceX);
    double stdDevY = Math.sqrt(varianceY);

    // Calculate correlation
    double correlation = 0.0;
    if (stdDevX > 0 && stdDevY > 0) {
      correlation = covariance / (stdDevX * stdDevY);
    }

    // Clamp to [-1, 1] to handle numerical errors
    correlation = Math.max(-1.0, Math.min(1.0, correlation));

    // Calculate R-squared
    double rSquared = correlation * correlation;

    // Calculate Beta
    double beta = varianceY > 0 ? covariance / varianceY : 1.0;

    return CorrelationResult.builder()
        .correlation(correlation)
        .rSquared(rSquared)
        .strength(CorrelationResult.determineStrength(correlation))
        .beta(beta)
        .isPositiveCorrelation(correlation > 0)
        .build();
  }

  private double calculateMean(List<Double> values, int length) {
    double sum = 0.0;
    int startIdx = values.size() - length;
    for (int i = startIdx; i < values.size(); i++) {
      sum += values.get(i);
    }
    return sum / length;
  }

  @Override
  public int getRequiredDataPoints() {
    return length;
  }

  /**
   * Input wrapper for correlation calculation.
   */
  public static class CorrelationInput {
    private final List<Double> primaryPrices;
    private final List<Double> comparisonPrices;

    public CorrelationInput(List<Double> primaryPrices, List<Double> comparisonPrices) {
      this.primaryPrices = primaryPrices;
      this.comparisonPrices = comparisonPrices;
    }

    public List<Double> getPrimaryPrices() {
      return primaryPrices;
    }

    public List<Double> getComparisonPrices() {
      return comparisonPrices;
    }
  }
}
