package com.trading.indicators.calculators;

import com.trading.indicators.model.ATMVega;
import java.util.List;

/**
 * ATM Vega Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def atmStrike = Round(close / strikeInterval) * strikeInterval;
 * def atmVega = option_vega(strike = atmStrike, expiration = dte);
 * def vegaPercentile = fold i = 0 to rankLen with count = 0 do if vegaHistory[i] < atmVega then count + 1 else count;
 * def vegaPercentileValue = vegaPercentile / rankLen * 100;
 * ```
 *
 * Note: This is a simplified calculation. Production implementations should
 * use actual options market data (greeks from broker/OCC).
 */
public class ATMVegaCalculator implements IndicatorCalculator<ATMVegaCalculator.ATMVegaInput, ATMVega> {

  private final int daysToExpiration;
  private final int rankLength;
  private final double highThreshold;
  private final double lowThreshold;
  private final double strikeInterval;

  public ATMVegaCalculator() {
    this(30, 252, 70.0, 30.0, 5.0);
  }

  /**
   * @param daysToExpiration Days to expiration for ATM options (default 30)
   * @param rankLength Lookback for percentile calculation
   * @param highThreshold Percentile threshold for "elevated"
   * @param lowThreshold Percentile threshold for "low"
   * @param strikeInterval Strike price interval for rounding (e.g., 5.0 for SPY)
   */
  public ATMVegaCalculator(int daysToExpiration, int rankLength,
                           double highThreshold, double lowThreshold,
                           double strikeInterval) {
    this.daysToExpiration = daysToExpiration;
    this.rankLength = rankLength;
    this.highThreshold = highThreshold;
    this.lowThreshold = lowThreshold;
    this.strikeInterval = strikeInterval;
  }

  @Override
  public ATMVega calculate(ATMVegaInput input) {
    double underlyingPrice = input.getUnderlyingPrice();
    List<Double> vegaHistory = input.getVegaHistory();

    // Calculate ATM strike
    double atmStrike = Math.round(underlyingPrice / strikeInterval) * strikeInterval;

    // Calculate ATM Vega (simplified Black-Scholes approximation)
    double atmVega = calculateSimplifiedVega(underlyingPrice, atmStrike, input.getIv(), daysToExpiration);

    // Calculate percentile and rank
    double vegaPercentile = calculatePercentile(vegaHistory, atmVega);
    double vegaRank = calculateRank(vegaHistory, atmVega);

    return ATMVega.builder()
        .atmVega(atmVega)
        .vegaPercentile(vegaPercentile)
        .vegaRank(vegaRank)
        .isVegaElevated(vegaPercentile >= highThreshold)
        .isVegaLow(vegaPercentile <= lowThreshold)
        .daysToExpiration(daysToExpiration)
        .underlyingPrice(underlyingPrice)
        .atmStrike(atmStrike)
        .build();
  }

  /**
   * Simplified ATM Vega calculation using Black-Scholes approximation.
   * For production, use actual option greeks from market data.
   *
   * ATM Vega approximation: S * sqrt(T) * N'(d1) / 100
   * Where d1 ~ 0 at ATM, so N'(0) = 1/sqrt(2*pi)
   */
  private double calculateSimplifiedVega(double underlying, double strike,
                                         double iv, int dte) {
    if (iv <= 0 || dte <= 0) {
      return 0.0;
    }

    double t = dte / 365.0; // Time to expiration in years
    double sqrtT = Math.sqrt(t);

    // Simplified ATM vega (per 1% change in IV)
    // Standardized for contract multiplier of 100
    double vega = underlying * sqrtT * 0.3989 / 100.0;

    return vega;
  }

  private double calculatePercentile(List<Double> history, double currentValue) {
    if (history == null || history.isEmpty()) {
      return 50.0;
    }

    int belowCount = 0;
    int count = 0;
    int startIdx = Math.max(0, history.size() - rankLength);

    for (int i = startIdx; i < history.size(); i++) {
      if (history.get(i) < currentValue) {
        belowCount++;
      }
      count++;
    }

    return count > 0 ? (belowCount / (double) count) * 100.0 : 50.0;
  }

  private double calculateRank(List<Double> history, double currentValue) {
    if (history == null || history.isEmpty()) {
      return 50.0;
    }

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    int startIdx = Math.max(0, history.size() - rankLength);
    for (int i = startIdx; i < history.size(); i++) {
      double v = history.get(i);
      min = Math.min(min, v);
      max = Math.max(max, v);
    }

    if (max == min) {
      return 50.0;
    }

    return ((currentValue - min) / (max - min)) * 100.0;
  }

  @Override
  public int getRequiredDataPoints() {
    return rankLength;
  }

  /**
   * Input wrapper for ATM Vega calculation.
   */
  public static class ATMVegaInput {
    private final double underlyingPrice;
    private final double iv;
    private final List<Double> vegaHistory;

    public ATMVegaInput(double underlyingPrice, double iv, List<Double> vegaHistory) {
      this.underlyingPrice = underlyingPrice;
      this.iv = iv;
      this.vegaHistory = vegaHistory;
    }

    public double getUnderlyingPrice() {
      return underlyingPrice;
    }

    public double getIv() {
      return iv;
    }

    public List<Double> getVegaHistory() {
      return vegaHistory;
    }
  }
}
