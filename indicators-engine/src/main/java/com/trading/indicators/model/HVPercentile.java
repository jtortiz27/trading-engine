package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Historical Volatility Percentile indicator.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - HV: historical_volatility(30)
 * - HV_Percentile: fold to count days HV was below current
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HVPercentile {
  /**
   * Historical Volatility (30-day annualized).
   * ThinkScript: historical_volatility(30)
   */
  private double historicalVolatility;

  /**
   * HV Percentile over rankLen period.
   * ThinkScript: count of days hv[i] < hv / rankLen * 100
   */
  private double hvPercentile;

  /**
   * HV Rank: (current - min) / (max - min) * 100.
   * ThinkScript: (hv - lowest(hv, rankLen)) / (highest(hv, rankLen) - lowest(hv, rankLen)) * 100
   */
  private double hvRank;

  /**
   * Is HV elevated (percentile > 80).
   */
  private boolean isHVElevated;

  /**
   * Is HV low (percentile < 20).
   */
  private boolean isHVLow;
}
