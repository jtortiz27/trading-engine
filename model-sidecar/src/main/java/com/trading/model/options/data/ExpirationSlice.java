package com.trading.model.options.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an expiration slice of options chain data.
 * Organizes strikes by expiration for analytics calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpirationSlice {
    private LocalDate expiryDate;
    private Integer daysToExpiry;
    private Double underlyingPrice;
    private String underlyingSymbol;
    private List<OptionStrike> strikes;
    
    // Computed metrics
    private Double atmIv;              // ATM implied volatility
    private Double forwardPrice;       // Forward price for this expiry
    private Double totalCallOi;        // Total call open interest
    private Double totalPutOi;         // Total put open interest
    private Double putCallRatio;       // Put/call OI ratio
    
    /**
     * Sorts strikes by strike price.
     */
    public void sortStrikes() {
        if (strikes != null) {
            strikes.sort(Comparator.comparing(OptionStrike::getStrike));
        }
    }
    
    /**
     * Gets the ATM strike (closest to underlying price).
     */
    public OptionStrike getAtmStrike() {
        if (strikes == null || strikes.isEmpty()) return null;
        return strikes.stream()
            .min(Comparator.comparing(s -> Math.abs(s.getStrike() - underlyingPrice)))
            .orElse(null);
    }
    
    /**
     * Gets strikes sorted by total OI (descending).
     */
    public List<OptionStrike> getStrikesByOi() {
        if (strikes == null) return Collections.emptyList();
        return strikes.stream()
            .sorted(Comparator.comparing(OptionStrike::getTotalOpenInterest, 
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets top N strikes by OI.
     */
    public List<OptionStrike> getTopOiStrikes(int n) {
        List<OptionStrike> sorted = getStrikesByOi();
        return sorted.subList(0, Math.min(n, sorted.size()));
    }
    
    /**
     * Calculates total open interest metrics.
     */
    public void calculateOiMetrics() {
        if (strikes == null) return;
        
        totalCallOi = strikes.stream()
            .mapToDouble(s -> s.getCallOpenInterest() != null ? s.getCallOpenInterest() : 0.0)
            .sum();
        totalPutOi = strikes.stream()
            .mapToDouble(s -> s.getPutOpenInterest() != null ? s.getPutOpenInterest() : 0.0)
            .sum();
        putCallRatio = totalCallOi > 0 ? totalPutOi / totalCallOi : 0.0;
        
        // Set ATM IV
        OptionStrike atm = getAtmStrike();
        if (atm != null) {
            atmIv = atm.getAverageIv();
        }
    }
    
    /**
     * Gets strikes within a delta range.
     */
    public List<OptionStrike> getStrikesByDeltaRange(double minDelta, double maxDelta, boolean isCall) {
        if (strikes == null) return Collections.emptyList();
        
        return strikes.stream()
            .filter(s -> {
                Double delta = isCall ? s.getCallDelta() : s.getPutDelta();
                if (delta == null) return false;
                double absDelta = Math.abs(delta);
                return absDelta >= minDelta && absDelta <= maxDelta;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the 25-delta strike (closest to 0.25 delta).
     */
    public OptionStrike get25DeltaStrike(boolean isCall) {
        if (strikes == null) return null;
        
        double targetDelta = isCall ? 0.25 : -0.25;
        return strikes.stream()
            .min(Comparator.comparing(s -> {
                Double delta = isCall ? s.getCallDelta() : s.getPutDelta();
                return delta != null ? Math.abs(delta - targetDelta) : Double.MAX_VALUE;
            }))
            .orElse(null);
    }
    
    /**
     * Gets the IV at a specific delta (interpolated if needed).
     */
    public Double getIvAtDelta(double targetDelta, boolean isCall) {
        List<OptionStrike> candidates = getStrikesByDeltaRange(
            targetDelta - 0.1, targetDelta + 0.1, isCall);
        
        if (candidates.isEmpty()) return atmIv;
        
        // Simple average of nearby strikes
        return candidates.stream()
            .mapToDouble(s -> {
                Double iv = isCall ? s.getCallIv() : s.getPutIv();
                return iv != null ? iv : 0.0;
            })
            .average()
            .orElse(atmIv != null ? atmIv : 0.0);
    }
    
    /**
     * Groups strikes by their dollar distance from ATM.
     */
    public Map<String, List<OptionStrike>> groupByDistance() {
        if (strikes == null) return Collections.emptyMap();
        
        OptionStrike atm = getAtmStrike();
        if (atm == null) return Collections.emptyMap();
        
        double atmStrike = atm.getStrike();
        
        return strikes.stream()
            .collect(Collectors.groupingBy(s -> {
                double dist = Math.abs(s.getStrike() - atmStrike);
                if (dist <= 5) return "near";
                if (dist <= 15) return "mid";
                return "far";
            }));
    }
}
