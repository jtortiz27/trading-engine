package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.IVDashboard;
import java.util.List;

/**
 * IV Dashboard Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def iv = implied_volatility();
 * def hv = historical_volatility(length);
 * def ivRank = (iv - lowest(iv, rankLen)) / (highest(iv, rankLen) - lowest(iv, rankLen)) * 100;
 * def ivHv = iv / hv;
 * def vrp = iv - hv;
 * ```
 */
public class IVDashboardCalculator implements IndicatorCalculator<List<Bar>, IVDashboard> {

  private final int hvLength;
  private final int rankLength;
  private final List<Double> ivHistory;

  /**
   * @param hvLength Lookback for historical volatility calculation
   * @param rankLength Lookback for IV rank/percentile
   * @param ivHistory Historical IV values (required for rank/percentile)
   */
  public IVDashboardCalculator(int hvLength, int rankLength, List<Double> ivHistory) {
    this.hvLength = hvLength;
    this.rankLength = rankLength;
    this.ivHistory = ivHistory;
  }

  public IVDashboardCalculator(int hvLength, int rankLength) {
    this(hvLength, rankLength, null);
  }

  @Override
  public IVDashboard calculate(List<Bar> bars) {
    if (bars == null || bars.size() < hvLength) {
      return IVDashboard.builder()
          .impliedVolatility(0.0)
          .ivRank(50.0)
          .ivPercentile(50.0)
          .ivHvRatio(1.0)
          .volatilityRiskPremium(0.0)
          .historicalVolatility(0.0)
          .isIVElevated(false)
          .isIVCheap(false)
          .build();
    }

    // Calculate Historical Volatility (HV) from price returns
    // ThinkScript: historical_volatility(length) uses close-to-close returns
    double hv = calculateHistoricalVolatility(bars, hvLength);

    // Current IV - in real implementation, this comes from options data
    // For now, we estimate or use provided IV history
    double currentIV = getCurrentIV();

    // Calculate IV Rank
    double ivRank = calculateIVRank(currentIV);

    // Calculate IV Percentile
    double ivPercentile = calculateIVPercentile(currentIV);

    // IV/HV Ratio
    double ivHvRatio = hv > 0 ? currentIV / hv : 1.0;

    // Volatility Risk Premium
    double vrp = currentIV - hv;

    return IVDashboard.builder()
        .impliedVolatility(currentIV)
        .ivRank(ivRank)
        .ivPercentile(ivPercentile)
        .ivHvRatio(ivHvRatio)
        .volatilityRiskPremium(vrp)
        .historicalVolatility(hv)
        .isIVElevated(ivRank > 50)
        .isIVCheap(ivRank < 30)
        .build();
  }

  /**
   * Calculate annualized historical volatility from close prices.
   * ThinkScript: historical_volatility(length) = StDev(log(close/close[1])) * Sqrt(252) * 100
   */
  private double calculateHistoricalVolatility(List<Bar> bars, int length) {
    if (bars.size() < length + 1) {
      return 0.0;
    }

    double sumLogReturns = 0.0;
    double sumSquaredLogReturns = 0.0;
    int count = 0;

    for (int i = bars.size() - length; i < bars.size(); i++) {
      double prevClose = bars.get(i - 1).getClose();
      double currClose = bars.get(i).getClose();

      if (prevClose > 0) {
        double logReturn = Math.log(currClose / prevClose);
        sumLogReturns += logReturn;
        sumSquaredLogReturns += logReturn * logReturn;
        count++;
      }
    }

    if (count < 2) {
      return 0.0;
    }

    double meanLogReturn = sumLogReturns / count;
    double variance = (sumSquaredLogReturns / count) - (meanLogReturn * meanLogReturn);
    double stdDev = Math.sqrt(variance);

    // Annualize: multiply by sqrt(252 trading days) and convert to percentage
    return stdDev * Math.sqrt(252) * 100;
  }

  /**
   * Calculate IV Rank: (current - low) / (high - low) * 100
   * ThinkScript: (iv - lowest(iv, rankLen)) / (highest(iv, rankLen) - lowest(iv, rankLen)) * 100
   */
  private double calculateIVRank(double currentIV) {
    if (ivHistory == null || ivHistory.size() < rankLength) {
      return 50.0; // Neutral when no history
    }

    double minIV = Double.MAX_VALUE;
    double maxIV = Double.MIN_VALUE;

    int startIdx = Math.max(0, ivHistory.size() - rankLength);
    for (int i = startIdx; i < ivHistory.size(); i++) {
      double iv = ivHistory.get(i);
      minIV = Math.min(minIV, iv);
      maxIV = Math.max(maxIV, iv);
    }

    if (maxIV == minIV) {
      return 50.0;
    }

    return ((currentIV - minIV) / (maxIV - minIV)) * 100.0;
  }

  /**
   * Calculate IV Percentile: percentage of days IV was below current.
   * ThinkScript: fold i = 0 to length with count = 0 do if iv[i] < iv then count + 1 else count
   */
  private double calculateIVPercentile(double currentIV) {
    if (ivHistory == null || ivHistory.isEmpty()) {
      return 50.0;
    }

    int belowCount = 0;
    int startIdx = Math.max(0, ivHistory.size() - rankLength);

    for (int i = startIdx; i < ivHistory.size(); i++) {
      if (ivHistory.get(i) < currentIV) {
        belowCount++;
      }
    }

    int total = ivHistory.size() - startIdx;
    return total > 0 ? (belowCount / (double) total) * 100.0 : 50.0;
  }

  /**
   * Get current IV. In production, this comes from options market data.
   * For testing, we can estimate from HV or use provided value.
   */
  private double getCurrentIV() {
    if (ivHistory != null && !ivHistory.isEmpty()) {
      return ivHistory.get(ivHistory.size() - 1);
    }
    return 30.0; // Default placeholder
  }

  @Override
  public int getRequiredDataPoints() {
    return hvLength;
  }
}
