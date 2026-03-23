package com.trading.model.options.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculates Realized vs Implied volatility metrics.
 * 
 * Key calculations:
 * - HV10, HV20 (close-to-close stdev * sqrt(252))
 * - VRP_ATM = IV_ATM² - HV20²
 * - Gap tape stats (median overnight gap, 90th pct)
 */
public class RealizedVolCalculator {
    
    private static final int TRADING_DAYS_YEAR = 252;
    private static final double ANNUALIZATION_FACTOR = Math.sqrt(TRADING_DAYS_YEAR);
    
    /**
     * Calculates historical volatility from a series of closing prices.
     * 
     * @param closes List of closing prices (most recent last)
     * @param period Number of days for HV calculation
     * @return Annualized historical volatility
     */
    public Double calculateHv(List<Double> closes, int period) {
        if (closes == null || closes.size() < period + 1 || period < 2) {
            return null;
        }
        
        // Get the last 'period' returns
        List<Double> returns = new ArrayList<>();
        List<Double> relevantCloses = closes.subList(closes.size() - period - 1, closes.size());
        
        for (int i = 1; i < relevantCloses.size(); i++) {
            double prevClose = relevantCloses.get(i - 1);
            double currClose = relevantCloses.get(i);
            if (prevClose > 0) {
                double logReturn = Math.log(currClose / prevClose);
                returns.add(logReturn);
            }
        }
        
        if (returns.isEmpty()) return null;
        
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
            .mapToDouble(r -> Math.pow(r - mean, 2))
            .average().orElse(0.0);
        
        // Annualized HV
        return Math.sqrt(variance) * ANNUALIZATION_FACTOR;
    }
    
    /**
     * Calculates HV10 (10-day historical volatility).
     */
    public Double calculateHv10(List<Double> closes) {
        return calculateHv(closes, 10);
    }
    
    /**
     * Calculates HV20 (20-day historical volatility).
     */
    public Double calculateHv20(List<Double> closes) {
        return calculateHv(closes, 20);
    }
    
    /**
     * Calculates VRP (Volatility Risk Premium) for ATM options.
     * VRP = IV_ATM² - HV20²
     * 
     * Positive VRP means implied vol > realized vol (sellers get paid)
     * Negative VRP means implied vol < realized vol (sellers lose)
     */
    public Double calculateVrp(double atmIv, double hv20) {
        return (atmIv * atmIv) - (hv20 * hv20);
    }
    
    /**
     * Calculates VRP percentage: (IV - HV) / HV * 100
     */
    public Double calculateVrpPercent(double atmIv, double hv20) {
        if (hv20 <= 0) return null;
        return ((atmIv - hv20) / hv20) * 100.0;
    }
    
    /**
     * Calculates overnight gap statistics.
     * 
     * @param opens List of opening prices
     * @param closes List of closing prices (previous day)
     * @return Gap statistics
     */
    public GapStatistics calculateGapStats(List<Double> opens, List<Double> closes) {
        if (opens == null || closes == null || opens.size() != closes.size() || opens.size() < 2) {
            return GapStatistics.empty();
        }
        
        List<Double> gaps = new ArrayList<>();
        
        for (int i = 1; i < opens.size(); i++) {
            double prevClose = closes.get(i - 1);
            double currOpen = opens.get(i);
            if (prevClose > 0) {
                double gap = (currOpen - prevClose) / prevClose * 100.0;
                gaps.add(gap);
            }
        }
        
        if (gaps.isEmpty()) return GapStatistics.empty();
        
        Collections.sort(gaps);
        
        GapStatistics stats = new GapStatistics();
        stats.setCount(gaps.size());
        stats.setMedian(percentile(gaps, 0.5));
        stats.setMean(gaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        stats.setPercentile10(percentile(gaps, 0.1));
        stats.setPercentile90(percentile(gaps, 0.9));
        stats.setMax(Collections.max(gaps));
        stats.setMin(Collections.min(gaps));
        stats.setStdDev(calculateStdDev(gaps));
        
        return stats;
    }
    
    /**
     * Calculates comprehensive realized vol metrics.
     */
    public RealizedVolResult calculate(List<Double> closes, List<Double> opens, double atmIv) {
        RealizedVolResult result = new RealizedVolResult();
        
        // Historical volatilities
        result.setHv10(calculateHv10(closes));
        result.setHv20(calculateHv20(closes));
        result.setHv30(calculateHv(closes, 30));
        
        // VRP calculations
        if (result.getHv20() != null) {
            result.setVrp(calculateVrp(atmIv, result.getHv20()));
            result.setVrpPercent(calculateVrpPercent(atmIv, result.getHv20()));
        }
        
        // Gap statistics
        if (opens != null) {
            result.setGapStats(calculateGapStats(opens, closes));
        }
        
        // Trend metrics
        result.setHv10ToHv20Ratio(result.getHv10() != null && result.getHv20() != null 
            && result.getHv20() > 0 ? result.getHv10() / result.getHv20() : null);
        
        return result;
    }
    
    /**
     * Calculates percentile value from sorted list.
     */
    private double percentile(List<Double> sorted, double percentile) {
        if (sorted == null || sorted.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
    
    /**
     * Calculates standard deviation.
     */
    private double calculateStdDev(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Result container for realized vol calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealizedVolResult {
        private Double hv10;
        private Double hv20;
        private Double hv30;
        private Double vrp;
        private Double vrpPercent;
        private GapStatistics gapStats;
        private Double hv10ToHv20Ratio;
        
        /**
         * Returns true if there's a volatility expansion (HV rising).
         */
        public boolean isVolExpanding() {
            return hv10ToHv20Ratio != null && hv10ToHv20Ratio > 1.15;
        }
        
        /**
         * Returns true if there's a volatility contraction (HV falling).
         */
        public boolean isVolContracting() {
            return hv10ToHv20Ratio != null && hv10ToHv20Ratio < 0.85;
        }
        
        /**
         * Returns true if VRP is favorable for selling options.
         */
        public boolean isVrpFavorableForSelling() {
            return vrp != null && vrp > 0;
        }
    }
    
    /**
     * Gap statistics container.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapStatistics {
        private int count;
        private double median;
        private double mean;
        private double percentile10;
        private double percentile90;
        private double max;
        private double min;
        private double stdDev;
        
        public static GapStatistics empty() {
            return GapStatistics.builder()
                .count(0)
                .median(0)
                .mean(0)
                .percentile10(0)
                .percentile90(0)
                .max(0)
                .min(0)
                .stdDev(0)
                .build();
        }
        
        /**
         * Returns the typical gap magnitude (absolute median).
         */
        public double getTypicalGapMagnitude() {
            return Math.abs(median);
        }
        
        /**
         * Returns true if gaps are typically large (> 1%).
         */
        public boolean hasLargeGaps() {
            return getTypicalGapMagnitude() > 1.0;
        }
    }
}
