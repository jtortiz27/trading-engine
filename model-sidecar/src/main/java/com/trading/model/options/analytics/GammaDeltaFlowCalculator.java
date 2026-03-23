package com.trading.model.options.analytics;

import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionStrike;
import com.trading.model.options.data.OptionsChainData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculates Gamma and Delta Flow metrics for options analysis.
 * 
 * Key calculations:
 * - Delta-weighted GEX per strike: Gamma * abs(Delta) * S / 100
 * - Zero-gamma flip levels (cumulative Delta-GEX)
 * - Net-ATM Gamma Tilt
 */
public class GammaDeltaFlowCalculator {
    
    private static final double GEX_SCALE = 100.0;
    
    /**
     * Calculates Gamma and Delta Flow metrics for an expiration slice.
     */
    public GammaDeltaFlowResult calculate(ExpirationSlice slice) {
        if (slice == null || slice.getStrikes() == null) {
            return GammaDeltaFlowResult.empty();
        }
        
        GammaDeltaFlowResult result = new GammaDeltaFlowResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        List<StrikeGex> strikeGexList = new ArrayList<>();
        double totalCallGex = 0.0;
        double totalPutGex = 0.0;
        
        // Calculate per-strike GEX
        for (OptionStrike strike : slice.getStrikes()) {
            StrikeGex sg = calculateStrikeGex(strike, slice.getUnderlyingPrice());
            if (sg != null) {
                strikeGexList.add(sg);
                totalCallGex += sg.getCallGex();
                totalPutGex += sg.getPutGex();
            }
        }
        
        // Sort by strike
        strikeGexList.sort(Comparator.comparing(StrikeGex::getStrike));
        result.setStrikeGexList(strikeGexList);
        result.setTotalCallGex(totalCallGex);
        result.setTotalPutGex(totalPutGex);
        result.setNetGex(totalCallGex + totalPutGex);
        
        // Find zero gamma flip levels
        List<Double> flipLevels = findZeroGammaFlipLevels(strikeGexList);
        result.setZeroGammaFlipLevels(flipLevels);
        
        // Find major GEX levels (maxima)
        List<Double> majorLevels = findMajorGexLevels(strikeGexList);
        result.setMajorGexLevels(majorLevels);
        
        // Calculate ATM Gamma Tilt
        double gammaTilt = calculateGammaTilt(strikeGexList, slice.getUnderlyingPrice());
        result.setAtmGammaTilt(gammaTilt);
        
        // Find nearest flip level
        double nearestFlip = findNearestFlipLevel(flipLevels, slice.getUnderlyingPrice());
        result.setNearestFlipLevel(nearestFlip);
        result.setFlipDistanceDollar(nearestFlip > 0 ? 
            Math.abs(nearestFlip - slice.getUnderlyingPrice()) : null);
        result.setFlipDistancePercent(slice.getUnderlyingPrice() > 0 ?
            result.getFlipDistanceDollar() / slice.getUnderlyingPrice() * 100.0 : null);
        
        return result;
    }
    
    /**
     * Calculates GEX for a single strike.
     * Formula: Gamma * abs(Delta) * Spot / 100
     */
    private StrikeGex calculateStrikeGex(OptionStrike strike, Double spot) {
        if (spot == null || spot <= 0) return null;
        
        StrikeGex sg = new StrikeGex();
        sg.setStrike(strike.getStrike());
        sg.setCallOi(strike.getCallOpenInterest() != null ? strike.getCallOpenInterest() : 0L);
        sg.setPutOi(strike.getPutOpenInterest() != null ? strike.getPutOpenInterest() : 0L);
        
        // Call GEX: Gamma * abs(Delta) * Spot / 100 * OI
        double callGex = 0.0;
        if (strike.getCallGamma() != null && strike.getCallDelta() != null && sg.getCallOi() > 0) {
            callGex = strike.getCallGamma() * Math.abs(strike.getCallDelta()) * spot / GEX_SCALE * sg.getCallOi();
        }
        sg.setCallGex(callGex);
        
        // Put GEX: Gamma * abs(Delta) * Spot / 100 * OI
        double putGex = 0.0;
        if (strike.getPutGamma() != null && strike.getPutDelta() != null && sg.getPutOi() > 0) {
            putGex = strike.getPutGamma() * Math.abs(strike.getPutDelta()) * spot / GEX_SCALE * sg.getPutOi();
        }
        sg.setPutGex(putGex);
        
        sg.setNetGex(callGex + putGex);
        
        // Store for convenience
        strike.setDeltaWeightedGex(sg.getNetGex());
        
        return sg;
    }
    
    /**
     * Finds zero gamma flip levels by looking for cumulative GEX sign changes.
     */
    private List<Double> findZeroGammaFlipLevels(List<StrikeGex> strikeGexList) {
        List<Double> flipLevels = new ArrayList<>();
        if (strikeGexList.size() < 2) return flipLevels;
        
        double cumulativeGex = 0.0;
        Double lastCumulative = null;
        
        for (int i = 0; i < strikeGexList.size(); i++) {
            cumulativeGex += strikeGexList.get(i).getNetGex();
            
            if (lastCumulative != null && 
                Math.signum(cumulativeGex) != Math.signum(lastCumulative) &&
                lastCumulative != 0) {
                // Sign change detected - interpolate
                double prevStrike = i > 0 ? strikeGexList.get(i - 1).getStrike() : 0;
                double currStrike = strikeGexList.get(i).getStrike();
                double interpolated = interpolateFlipLevel(
                    prevStrike, lastCumulative, currStrike, cumulativeGex);
                flipLevels.add(interpolated);
            }
            
            lastCumulative = cumulativeGex;
        }
        
        return flipLevels;
    }
    
    /**
     * Interpolates the exact flip level between two strikes.
     */
    private double interpolateFlipLevel(double strike1, double gex1, double strike2, double gex2) {
        if (Math.abs(gex2 - gex1) < 1e-10) return strike1;
        double t = Math.abs(gex1) / Math.abs(gex2 - gex1);
        return strike1 + t * (strike2 - strike1);
    }
    
    /**
     * Finds major GEX levels (local maxima in absolute GEX).
     */
    private List<Double> findMajorGexLevels(List<StrikeGex> strikeGexList) {
        List<Double> majorLevels = new ArrayList<>();
        if (strikeGexList.size() < 3) return majorLevels;
        
        for (int i = 1; i < strikeGexList.size() - 1; i++) {
            double prevGex = Math.abs(strikeGexList.get(i - 1).getNetGex());
            double currGex = Math.abs(strikeGexList.get(i).getNetGex());
            double nextGex = Math.abs(strikeGexList.get(i + 1).getNetGex());
            
            if (currGex > prevGex && currGex > nextGex && currGex > 100000) {
                majorLevels.add(strikeGexList.get(i).getStrike());
            }
        }
        
        return majorLevels;
    }
    
    /**
     * Calculates Net-ATM Gamma Tilt.
     * Measures whether ATM options have net long or short gamma exposure.
     * Positive = dealers long gamma (market stabilizing)
     * Negative = dealers short gamma (market destabilizing)
     */
    private double calculateGammaTilt(List<StrikeGex> strikeGexList, Double spot) {
        if (spot == null || strikeGexList.isEmpty()) return 0.0;
        
        // Find ATM and surrounding strikes (within 2%)
        double atmGex = 0.0;
        double totalWeight = 0.0;
        
        for (StrikeGex sg : strikeGexList) {
            double distance = Math.abs(sg.getStrike() - spot) / spot;
            if (distance <= 0.02) {
                double weight = 1.0 - distance / 0.02; // Higher weight for closer strikes
                atmGex += sg.getNetGex() * weight;
                totalWeight += weight;
            }
        }
        
        return totalWeight > 0 ? atmGex / totalWeight : 0.0;
    }
    
    /**
     * Finds the nearest zero gamma flip level to current price.
     */
    private double findNearestFlipLevel(List<Double> flipLevels, Double spot) {
        if (flipLevels == null || flipLevels.isEmpty() || spot == null) return 0.0;
        
        return flipLevels.stream()
            .min(Comparator.comparing(level -> Math.abs(level - spot)))
            .orElse(0.0);
    }
    
    /**
     * Result container for Gamma-Delta Flow calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GammaDeltaFlowResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private List<StrikeGex> strikeGexList;
        private double totalCallGex;
        private double totalPutGex;
        private double netGex;
        private List<Double> zeroGammaFlipLevels;
        private List<Double> majorGexLevels;
        private double atmGammaTilt;
        private double nearestFlipLevel;
        private Double flipDistanceDollar;
        private Double flipDistancePercent;
        
        public static GammaDeltaFlowResult empty() {
            return GammaDeltaFlowResult.builder()
                .strikeGexList(Collections.emptyList())
                .zeroGammaFlipLevels(Collections.emptyList())
                .majorGexLevels(Collections.emptyList())
                .build();
        }
    }
    
    /**
     * Per-strike GEX data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrikeGex {
        private double strike;
        private long callOi;
        private long putOi;
        private double callGex;
        private double putGex;
        private double netGex;
    }
}
