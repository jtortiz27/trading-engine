package com.trading.model.options.data;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single option strike with all relevant market data and Greeks.
 * Used as the primary data structure for options analytics calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionStrike {
    private String underlyingSymbol;
    private Double underlyingPrice;
    private Double strike;
    private LocalDate expiryDate;
    private Integer daysToExpiry;
    
    // Contract identification
    private String callSymbol;
    private String putSymbol;
    
    // Prices
    private Double callBid;
    private Double callAsk;
    private Double callLast;
    private Double putBid;
    private Double putAsk;
    private Double putLast;
    
    // Greeks - Calls
    private Double callDelta;
    private Double callGamma;
    private Double callTheta;
    private Double callVega;
    private Double callRho;
    
    // Greeks - Puts
    private Double putDelta;
    private Double putGamma;
    private Double putTheta;
    private Double putVega;
    private Double putRho;
    
    // Implied Volatility
    private Double callIv;
    private Double putIv;
    private Double midIv;
    
    // Open Interest
    private Long callOpenInterest;
    private Long putOpenInterest;
    private Long totalOpenInterest;
    
    // Volume
    private Long callVolume;
    private Long putVolume;
    
    // Computed metrics (populated by analytics)
    private Double gammaExposure;      // Gamma * OI * Spot
    private Double deltaWeightedGex;   // Gamma * abs(Delta) * Spot / 100
    private Double callPutOiRatio;
    private Double netGamma;
    
    /**
     * Returns the mid price for calls.
     */
    public Double getCallMid() {
        if (callBid == null || callAsk == null) return callLast;
        if (callBid <= 0 || callAsk <= 0) return callLast;
        return (callBid + callAsk) / 2.0;
    }
    
    /**
     * Returns the mid price for puts.
     */
    public Double getPutMid() {
        if (putBid == null || putAsk == null) return putLast;
        if (putBid <= 0 || putAsk <= 0) return putLast;
        return (putBid + putAsk) / 2.0;
    }
    
    /**
     * Returns the spread as a percentage of the mid price for calls.
     */
    public Double getCallSpreadPct() {
        Double mid = getCallMid();
        if (mid == null || mid <= 0 || callBid == null || callAsk == null) return null;
        return ((callAsk - callBid) / mid) * 100.0;
    }
    
    /**
     * Returns the spread as a percentage of the mid price for puts.
     */
    public Double getPutSpreadPct() {
        Double mid = getPutMid();
        if (mid == null || mid <= 0 || putBid == null || putAsk == null) return null;
        return ((putAsk - putBid) / mid) * 100.0;
    }
    
    /**
     * Returns true if either call or put is "thin" (wide spreads, low liquidity).
     */
    public boolean isThin() {
        Double callSpread = getCallSpreadPct();
        Double putSpread = getPutSpreadPct();
        
        // Consider thin if spread > 10% or no valid price
        boolean callThin = callSpread == null || callSpread > 10.0;
        boolean putThin = putSpread == null || putSpread > 10.0;
        
        return callThin || putThin;
    }
    
    /**
     * Distance from underlying price in dollars.
     */
    public Double getDollarDistance() {
        if (underlyingPrice == null || strike == null) return null;
        return Math.abs(strike - underlyingPrice);
    }
    
    /**
     * Distance from underlying price in percentage.
     */
    public Double getPercentDistance() {
        if (underlyingPrice == null || underlyingPrice <= 0) return null;
        Double dist = getDollarDistance();
        return dist != null ? (dist / underlyingPrice) * 100.0 : null;
    }
    
    /**
     * Returns true if this strike is at-the-money (within 1% of spot).
     */
    public boolean isAtm() {
        Double pctDist = getPercentDistance();
        return pctDist != null && pctDist <= 1.0;
    }
    
    /**
     * Calculates net gamma at this strike (call gamma + put gamma weighted by OI).
     */
    public void calculateNetGamma() {
        double callG = callGamma != null && callOpenInterest != null 
            ? callGamma * callOpenInterest : 0.0;
        double putG = putGamma != null && putOpenInterest != null 
            ? putGamma * putOpenInterest : 0.0;
        this.netGamma = callG + putG;
    }
    
    /**
     * Calculates delta-weighted GEX per strike: Gamma * abs(Delta) * S / 100
     */
    public void calculateDeltaWeightedGex() {
        if (underlyingPrice == null) {
            this.deltaWeightedGex = 0.0;
            return;
        }
        
        double callGex = 0.0;
        if (callGamma != null && callDelta != null && callOpenInterest != null) {
            callGex = callGamma * Math.abs(callDelta) * underlyingPrice / 100.0 * callOpenInterest;
        }
        
        double putGex = 0.0;
        if (putGamma != null && putDelta != null && putOpenInterest != null) {
            putGex = putGamma * Math.abs(putDelta) * underlyingPrice / 100.0 * putOpenInterest;
        }
        
        this.deltaWeightedGex = callGex + putGex;
    }
    
    /**
     * Returns the average IV across call and put.
     */
    public Double getAverageIv() {
        if (callIv != null && putIv != null) {
            return (callIv + putIv) / 2.0;
        }
        return callIv != null ? callIv : putIv;
    }
}
