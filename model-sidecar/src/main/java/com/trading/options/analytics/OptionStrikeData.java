package com.trading.options.analytics;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents option data at a specific strike for a given expiration.
 * Combines call and put data with Greeks and market data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionStrikeData {

  private Double strike;
  private LocalDate expiry;
  private Double daysToExpiry;

  // Call data
  private Double callBid;
  private Double callAsk;
  private Double callLast;
  private Long callVolume;
  private Long callOpenInterest;
  private Double callIv;
  private Double callDelta;
  private Double callGamma;
  private Double callTheta;
  private Double callVega;
  private Double callRho;

  // Put data
  private Double putBid;
  private Double putAsk;
  private Double putLast;
  private Long putVolume;
  private Long putOpenInterest;
  private Double putIv;
  private Double putDelta;
  private Double putGamma;
  private Double putTheta;
  private Double putVega;
  private Double putRho;

  // Computed fields
  private Double netOpenInterest; // call OI + put OI
  private Double netGamma; // call gamma + put gamma
  private Double deltaWeightedGex; // Gamma * abs(Delta) * Spot / 100

  /**
   * Gets the mid price for calls.
   */
  public Double getCallMid() {
    if (callBid == null || callAsk == null) return null;
    return (callBid + callAsk) / 2.0;
  }

  /**
   * Gets the mid price for puts.
   */
  public Double getPutMid() {
    if (putBid == null || putAsk == null) return null;
    return (putBid + putAsk) / 2.0;
  }

  /**
   * Gets the bid/ask spread width as a percentage for calls.
   */
  public Double getCallSpreadPct(Double spot) {
    if (callBid == null || callAsk == null || callBid <= 0 || spot <= 0) return null;
    return (callAsk - callBid) / spot * 100.0;
  }

  /**
   * Gets the bid/ask spread width as a percentage for puts.
   */
  public Double getPutSpreadPct(Double spot) {
    if (putBid == null || putAsk == null || putBid <= 0 || spot <= 0) return null;
    return (putAsk - putBid) / spot * 100.0;
  }

  /**
   * Calculates total GEX at this strike (Gamma Exposure).
   * GEX = Gamma * OpenInterest * ContractMultiplier (100)
   */
  public Double getTotalGex() {
    double callGex = callGamma != null && callOpenInterest != null
        ? callGamma * callOpenInterest * 100.0 : 0.0;
    double putGex = putGamma != null && putOpenInterest != null
        ? putGamma * putOpenInterest * 100.0 : 0.0;
    return callGex + putGex;
  }

  /**
   * Calculates Delta-weighted GEX per the spec:
   * Delta-weighted GEX = Gamma * abs(Delta) * Spot / 100
   */
  public Double calculateDeltaWeightedGex(Double spot) {
    if (spot == null) return null;

    double callDgex = 0.0;
    if (callGamma != null && callDelta != null && callOpenInterest != null) {
      callDgex = callGamma * Math.abs(callDelta) * spot / 100.0 * callOpenInterest;
    }

    double putDgex = 0.0;
    if (putGamma != null && putDelta != null && putOpenInterest != null) {
      putDgex = putGamma * Math.abs(putDelta) * spot / 100.0 * putOpenInterest;
    }

    return callDgex + putDgex;
  }

  /**
   * Gets the total open interest (calls + puts).
   */
  public Double getTotalOpenInterest() {
    double callOi = callOpenInterest != null ? callOpenInterest.doubleValue() : 0.0;
    double putOi = putOpenInterest != null ? putOpenInterest.doubleValue() : 0.0;
    return callOi + putOi;
  }

  /**
   * Gets the combined gamma (calls + puts).
   */
  public Double getCombinedGamma() {
    double callG = callGamma != null ? callGamma : 0.0;
    double putG = putGamma != null ? putGamma : 0.0;
    return callG + putG;
  }

  /**
   * Calculates the put/call ratio for open interest.
   */
  public Double getPutCallRatioOi() {
    double callOi = callOpenInterest != null ? callOpenInterest.doubleValue() : 0.0;
    double putOi = putOpenInterest != null ? putOpenInterest.doubleValue() : 0.0;
    if (callOi <= 0) return null;
    return putOi / callOi;
  }
}
