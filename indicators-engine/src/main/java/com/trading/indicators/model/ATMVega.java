package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ATM Vega indicator result.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - ATM_Vega: option_vega for nearest ATM strike
 * - Vega_Percentile: percentile over lookback period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ATMVega {
  /**
   * Current ATM Vega value.
   * ThinkScript: option_vega(strike = ATM(), expiration = 30)
   */
  private double atmVega;

  /**
   * ATM Vega percentile over lookback period.
   * ThinkScript: fold to count days vega[i] < vega
   */
  private double vegaPercentile;

  /**
   * ATM Vega rank (0-100).
   * ThinkScript: (vega - lowest(vega, len)) / (highest(vega, len) - lowest(vega, len)) * 100
   */
  private double vegaRank;

  /**
   * Is Vega elevated (percentile > 70).
   */
  private boolean isVegaElevated;

  /**
   * Is Vega low (percentile < 30).
   */
  private boolean isVegaLow;

  /**
   * Days to expiration for the ATM option.
   */
  private int daysToExpiration;

  /**
   * Current underlying price at calculation time.
   */
  private double underlyingPrice;

  /**
   * Nearest ATM strike price.
   */
  private double atmStrike;
}
