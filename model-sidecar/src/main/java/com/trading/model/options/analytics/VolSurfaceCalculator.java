package com.trading.model.options.analytics;

import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionStrike;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculates Volatility Surface Shape metrics for options analysis.
 * 
 * Key calculations:
 * - RR25 (Risk Reversal): IV(25d call) - IV(25d put)
 * - BF25 (Butterfly): 0.5*(IV(25d call)+IV(25d put)) - IV(ATM)
 * - Term slope: IV(~30D) - IV(~60D)
 * - Wing-richness z-score
 */
public class VolSurfaceCalculator {
    
    private static final double DELTA_25 = 0.25;
    private static final int DTE_30 = 30;
    private static final int DTE_60 = 60;
    
    /**
     * Calculates vol surface metrics for an expiration slice.
     */
    public VolSurfaceResult calculate(ExpirationSlice slice) {
        if (slice == null || slice.getStrikes() == null || slice.getStrikes().isEmpty()) {
            return VolSurfaceResult.empty();
        }
        
        VolSurfaceResult result = new VolSurfaceResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        // Get ATM IV
        OptionStrike atmStrike = slice.getAtmStrike();
        double atmIv = atmStrike != null ? atmStrike.getAverageIv() : 0.0;
        result.setAtmIv(atmIv);
        
        // Find 25 delta strikes and their IVs
        StrikeIv delta25Call = findStrikeAtDelta(slice.getStrikes(), DELTA_25, true);
        StrikeIv delta25Put = findStrikeAtDelta(slice.getStrikes(), DELTA_25, false);
        
        if (delta25Call != null && delta25Put != null) {
            // Risk Reversal
            double rr25 = delta25Call.getIv() - delta25Put.getIv();
            result.setRiskReversal25(rr25);
            result.setRiskReversal25CallStrike(delta25Call.getStrike());
            result.setRiskReversal25PutStrike(delta25Put.getStrike());
            
            // Butterfly
            double bf25 = 0.5 * (delta25Call.getIv() + delta25Put.getIv()) - atmIv;
            result.setButterfly25(bf25);
        }
        
        // Find 10 delta strikes for wing analysis
        StrikeIv delta10Call = findStrikeAtDelta(slice.getStrikes(), 0.10, true);
        StrikeIv delta10Put = findStrikeAtDelta(slice.getStrikes(), 0.10, false);
        
        if (delta10Call != null && delta10Put != null) {
            double rr10 = delta10Call.getIv() - delta10Put.getIv();
            result.setRiskReversal10(rr10);
            result.setWingSkew(delta10Call.getIv() + delta10Put.getIv() - 2 * atmIv);
        }
        
        // Calculate slope of IV across strikes
        result.setIvSlope(calculateIvSlope(slice.getStrikes(), atmStrike));
        
        // Calculate term structure if multiple expirations
        // (This is typically done across expirations, not within one)
        
        return result;
    }
    
    /**
     * Calculates term structure metrics across expiration slices.
     */
    public TermStructureResult calculateTermStructure(List<ExpirationSlice> slices) {
        TermStructureResult result = new TermStructureResult();
        if (slices == null || slices.size() < 2) return result;
        
        // Sort by DTE
        List<ExpirationSlice> sorted = new ArrayList<>(slices);
        sorted.sort(Comparator.comparing(ExpirationSlice::getDaysToExpiry));
        
        // Find ~30D and ~60D expirations
        ExpirationSlice slice30 = findClosestByDte(sorted, DTE_30);
        ExpirationSlice slice60 = findClosestByDte(sorted, DTE_60);
        
        if (slice30 != null && slice60 != null) {
            double iv30 = slice30.getAtmIv() != null ? slice30.getAtmIv() : 0.0;
            double iv60 = slice60.getAtmIv() != null ? slice60.getAtmIv() : 0.0;
            
            result.setDte30(iv30);
            result.setDte60(iv60);
            result.setTermSlope(iv30 - iv60);
            result.setTermSlopeAnnualized((iv30 - iv60) / (slice60.getDaysToExpiry() - slice30.getDaysToExpiry()) * 30);
        }
        
        // Calculate wing richness z-score
        result.setWingRichnessZscore(calculateWingRichnessZscore(sorted));
        
        return result;
    }
    
    /**
     * Finds the strike closest to target delta and returns its IV.
     */
    private StrikeIv findStrikeAtDelta(List<OptionStrike> strikes, double targetDelta, boolean isCall) {
        OptionStrike bestStrike = null;
        double bestDeltaDiff = Double.MAX_VALUE;
        double bestIv = 0.0;
        
        for (OptionStrike strike : strikes) {
            Double delta = isCall ? strike.getCallDelta() : strike.getPutDelta();
            Double iv = isCall ? strike.getCallIv() : strike.getPutIv();
            
            if (delta == null || iv == null) continue;
            
            double adjustedTarget = isCall ? targetDelta : -targetDelta;
            double deltaDiff = Math.abs(delta - adjustedTarget);
            
            if (deltaDiff < bestDeltaDiff) {
                bestDeltaDiff = deltaDiff;
                bestStrike = strike;
                bestIv = iv;
            }
        }
        
        if (bestStrike == null) return null;
        
        return new StrikeIv(bestStrike.getStrike(), bestIv);
    }
    
    /**
     * Calculates IV slope (change per strike distance).
     */
    private Double calculateIvSlope(List<OptionStrike> strikes, OptionStrike atmStrike) {
        if (strikes == null || strikes.isEmpty() || atmStrike == null) return null;
        
        // Find strikes above and below ATM
        List<OptionStrike> sorted = new ArrayList<>(strikes);
        sorted.sort(Comparator.comparing(OptionStrike::getStrike));
        
        int atmIndex = sorted.indexOf(atmStrike);
        if (atmIndex < 0 || atmIndex >= sorted.size() - 1) return null;
        
        // Calculate average slope
        double totalSlope = 0.0;
        int count = 0;
        
        for (int i = 1; i < sorted.size(); i++) {
            OptionStrike lower = sorted.get(i - 1);
            OptionStrike upper = sorted.get(i);
            
            Double lowerIv = lower.getAverageIv();
            Double upperIv = upper.getAverageIv();
            
            if (lowerIv != null && upperIv != null) {
                double ivDiff = upperIv - lowerIv;
                double strikeDiff = upper.getStrike() - lower.getStrike();
                if (strikeDiff > 0) {
                    totalSlope += ivDiff / strikeDiff;
                    count++;
                }
            }
        }
        
        return count > 0 ? totalSlope / count : null;
    }
    
    /**
     * Finds the expiration slice closest to target DTE.
     */
    private ExpirationSlice findClosestByDte(List<ExpirationSlice> slices, int targetDte) {
        return slices.stream()
            .min(Comparator.comparing(s -> Math.abs(s.getDaysToExpiry() - targetDte)))
            .orElse(null);
    }
    
    /**
     * Calculates wing richness z-score across expiration slices.
     * Compares OTM wing IV to ATM IV relative to historical norm.
     * 
     * Positive = wings are expensive (higher IV in OTM options)
     * Negative = wings are cheap (lower IV in OTM options)
     */
    private double calculateWingRichnessZscore(List<ExpirationSlice> slices) {
        if (slices == null || slices.isEmpty()) return 0.0;
        
        List<Double> wingRatios = new ArrayList<>();
        
        for (ExpirationSlice slice : slices) {
            if (slice.getStrikes() == null) continue;
            
            OptionStrike atm = slice.getAtmStrike();
            if (atm == null) continue;
            
            double atmIv = atm.getAverageIv() != null ? atm.getAverageIv() : 0.0;
            if (atmIv <= 0) continue;
            
            // Find 25 delta wings
            StrikeIv call25 = findStrikeAtDelta(slice.getStrikes(), 0.25, true);
            StrikeIv put25 = findStrikeAtDelta(slice.getStrikes(), 0.25, false);
            
            if (call25 != null && put25 != null) {
                double wingAvg = (call25.getIv() + put25.getIv()) / 2.0;
                double ratio = wingAvg / atmIv;
                wingRatios.add(ratio);
            }
        }
        
        if (wingRatios.isEmpty()) return 0.0;
        
        // Calculate z-score
        double mean = wingRatios.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        double variance = wingRatios.stream()
            .mapToDouble(r -> Math.pow(r - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        if (stdDev < 1e-10) return 0.0;
        
        // Z-score of most recent (last in sorted list)
        double latest = wingRatios.get(wingRatios.size() - 1);
        return (latest - mean) / stdDev;
    }
    
    /**
     * Helper class to hold strike price and IV.
     */
    @Data
    @AllArgsConstructor
    private static class StrikeIv {
        private double strike;
        private double iv;
    }
    
    /**
     * Result container for vol surface calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolSurfaceResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private double atmIv;
        private Double riskReversal25;
        private Double riskReversal25CallStrike;
        private Double riskReversal25PutStrike;
        private Double butterfly25;
        private Double riskReversal10;
        private Double wingSkew;
        private Double ivSlope;
        
        public static VolSurfaceResult empty() {
            return VolSurfaceResult.builder().build();
        }
    }
    
    /**
     * Result container for term structure calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermStructureResult {
        private double dte30;
        private double dte60;
        private Double termSlope;
        private Double termSlopeAnnualized;
        private Double wingRichnessZscore;
        
        public boolean isInverted() {
            return termSlope != null && termSlope < 0;
        }
    }
}
