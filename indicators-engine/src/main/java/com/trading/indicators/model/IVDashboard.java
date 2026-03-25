package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Implied Volatility Dashboard.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - IV: implied_volatility()
 * - IV_Rank: (currentIV - lowIV) / (highIV - lowIV) * 100
 * - IV/HV: implied / historical
 * - VRP: IV - HV (volatility risk premium)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IVDashboard {
  /**
   * Current Implied Volatility (IV).
   * ThinkScript: implied_volatility()
   */
  private double impliedVolatility;

  /**
   * IV Rank: percentile of current IV over lookback period.
   * ThinkScript: (iv - lowest(iv, length)) / (highest(iv, length) - lowest(iv, length)) * 100
   */
  private double ivRank;

  /**
   * IV Percentile: percentage of days IV was below current.
   * ThinkScript: fold i = 0 to length with count = 0 do if iv[i] < iv then count + 1 else count
   */
  private double ivPercentile;

  /**
   * IV/HV Ratio: implied volatility / historical volatility.
   * ThinkScript: implied_volatility() / historical_volatility(length)
   */
  private double ivHvRatio;

  /**
   * Volatility Risk Premium: IV - HV.
   * ThinkScript: implied_volatility() - historical_volatility(length)
   */
  private double volatilityRiskPremium;

  /**
   * Historical Volatility used in calculations.
   */
  private double historicalVolatility;

  /**
   * Is IV elevated (IV Rank > 50).
   */
  private boolean isIVElevated;

  /**
   * Is IV cheap (IV Rank < 30).
   */
  private boolean isIVCheap;
}
