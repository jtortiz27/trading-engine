package com.trading.model.options.analytics;

import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionStrike;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculates Liquidity and Quote Quality metrics for options analysis.
 * 
 * Key calculations:
 * - Median and Max bid/ask width % across top-10 OI strikes
 * - "Thin" flag logic
 */
public class LiquidityCalculator {
    
    // Thresholds for thin markets
    private static final double THIN_SPREAD_PCT = 10.0;      // Spread > 10%
    private static final double WIDE_SPREAD_PCT = 20.0;      // Spread > 20%
    private static final int MIN_TOP_STRIKES = 10;
    
    /**
     * Calculates liquidity metrics for an expiration slice.
     */
    public LiquidityResult calculate(ExpirationSlice slice) {
        if (slice == null || slice.getStrikes() == null || slice.getStrikes().isEmpty()) {
            return LiquidityResult.empty();
        }
        
        LiquidityResult result = new LiquidityResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        // Get top strikes by OI
        List<OptionStrike> topStrikes = slice.getTopOiStrikes(MIN_TOP_STRIKES);
        result.setNumStrikesAnalyzed(topStrikes.size());
        
        // Calculate spread statistics for calls
        SpreadStats callStats = calculateSpreadStats(topStrikes, true);
        result.setCallSpreadStats(callStats);
        
        // Calculate spread statistics for puts
        SpreadStats putStats = calculateSpreadStats(topStrikes, false);
        result.setPutSpreadStats(putStats);
        
        // Combined stats
        result.setCombinedMedianSpread((callStats.getMedianSpread() + putStats.getMedianSpread()) / 2.0);
        result.setCombinedMaxSpread(Math.max(callStats.getMaxSpread(), putStats.getMaxSpread()));
        
        // Determine if thin
        result.setThin(determineIfThin(result));
        result.setThinReasons(determineThinReasons(result, topStrikes));
        
        // Calculate liquidity score (0-100, higher = more liquid)
        result.setLiquidityScore(calculateLiquidityScore(result));
        
        // ATM liquidity
        OptionStrike atm = slice.getAtmStrike();
        if (atm != null) {
            result.setAtmCallSpread(atm.getCallSpreadPct());
            result.setAtmPutSpread(atm.getPutSpreadPct());
        }
        
        return result;
    }
    
    /**
     * Calculates spread statistics for a set of strikes.
     */
    private SpreadStats calculateSpreadStats(List<OptionStrike> strikes, boolean isCall) {
        SpreadStats stats = new SpreadStats();
        List<Double> spreads = new ArrayList<>();
        
        for (OptionStrike strike : strikes) {
            Double spread = isCall ? strike.getCallSpreadPct() : strike.getPutSpreadPct();
            if (spread != null && spread > 0) {
                spreads.add(spread);
            }
        }
        
        if (spreads.isEmpty()) {
            stats.setMedianSpread(100.0); // Worst case
            stats.setMaxSpread(100.0);
            stats.setMeanSpread(100.0);
            stats.setNumValidStrikes(0);
            return stats;
        }
        
        spreads.sort(Double::compareTo);
        
        stats.setNumValidStrikes(spreads.size());
        stats.setMinSpread(spreads.get(0));
        stats.setMaxSpread(spreads.get(spreads.size() - 1));
        stats.setMedianSpread(calculateMedian(spreads));
        stats.setMeanSpread(spreads.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        
        // Count problematic strikes
        long thinCount = spreads.stream().filter(s -> s > THIN_SPREAD_PCT).count();
        long wideCount = spreads.stream().filter(s -> s > WIDE_SPREAD_PCT).count();
        
        stats.setThinStrikesCount((int) thinCount);
        stats.setWideStrikesCount((int) wideCount);
        stats.setThinStrikesPercent((thinCount * 100.0) / spreads.size());
        
        return stats;
    }
    
    /**
     * Calculates median from sorted list.
     */
    private double calculateMedian(List<Double> sorted) {
        int size = sorted.size();
        if (size == 0) return 0.0;
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }
    
    /**
     * Determines if the market should be flagged as "thin".
     */
    private boolean determineIfThin(LiquidityResult result) {
        SpreadStats callStats = result.getCallSpreadStats();
        SpreadStats putStats = result.getPutSpreadStats();
        
        // Thin if median spread > 10%
        if (callStats.getMedianSpread() > THIN_SPREAD_PCT || 
            putStats.getMedianSpread() > THIN_SPREAD_PCT) {
            return true;
        }
        
        // Thin if max spread > 30%
        if (callStats.getMaxSpread() > 30.0 || putStats.getMaxSpread() > 30.0) {
            return true;
        }
        
        // Thin if more than 40% of strikes have wide spreads
        if (callStats.getThinStrikesPercent() > 40.0 || putStats.getThinStrikesPercent() > 40.0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines reasons for thin flag.
     */
    private List<String> determineThinReasons(LiquidityResult result, List<OptionStrike> topStrikes) {
        List<String> reasons = new ArrayList<>();
        SpreadStats callStats = result.getCallSpreadStats();
        SpreadStats putStats = result.getPutSpreadStats();
        
        if (callStats.getMedianSpread() > THIN_SPREAD_PCT) {
            reasons.add("High median call spread: " + String.format("%.1f%%", callStats.getMedianSpread()));
        }
        if (putStats.getMedianSpread() > THIN_SPREAD_PCT) {
            reasons.add("High median put spread: " + String.format("%.1f%%", putStats.getMedianSpread()));
        }
        if (callStats.getMaxSpread() > WIDE_SPREAD_PCT || putStats.getMaxSpread() > WIDE_SPREAD_PCT) {
            reasons.add("Wide spreads on some strikes");
        }
        if (callStats.getThinStrikesPercent() > 40.0 || putStats.getThinStrikesPercent() > 40.0) {
            reasons.add(String.format("%.0f%% of strikes have wide spreads", 
                Math.max(callStats.getThinStrikesPercent(), putStats.getThinStrikesPercent())));
        }
        if (result.getNumStrikesAnalyzed() < MIN_TOP_STRIKES / 2) {
            reasons.add("Limited strikes with meaningful OI");
        }
        
        return reasons;
    }
    
    /**
     * Calculates a liquidity score (0-100, higher = more liquid).
     */
    private int calculateLiquidityScore(LiquidityResult result) {
        SpreadStats callStats = result.getCallSpreadStats();
        SpreadStats putStats = result.getPutSpreadStats();
        
        // Base score from median spreads
        double medianSpread = (callStats.getMedianSpread() + putStats.getMedianSpread()) / 2.0;
        double score = Math.max(0, 100 - medianSpread * 10);
        
        // Penalize for wide max spreads
        double maxSpread = Math.max(callStats.getMaxSpread(), putStats.getMaxSpread());
        if (maxSpread > 20.0) {
            score -= (maxSpread - 20.0) * 2;
        }
        
        // Penalize for thin strike percentage
        double thinPct = Math.max(callStats.getThinStrikesPercent(), putStats.getThinStrikesPercent());
        score -= thinPct * 0.5;
        
        // Bonus for sufficient strikes analyzed
        if (result.getNumStrikesAnalyzed() >= MIN_TOP_STRIKES) {
            score += 5;
        }
        
        return (int) Math.max(0, Math.min(100, score));
    }
    
    /**
     * Result container for liquidity calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private int numStrikesAnalyzed;
        private SpreadStats callSpreadStats;
        private SpreadStats putSpreadStats;
        private Double combinedMedianSpread;
        private Double combinedMaxSpread;
        private boolean thin;
        private List<String> thinReasons;
        private int liquidityScore;
        private Double atmCallSpread;
        private Double atmPutSpread;
        
        public static LiquidityResult empty() {
            return LiquidityResult.builder()
                .thin(true)
                .thinReasons(new ArrayList<>())
                .callSpreadStats(new SpreadStats())
                .putSpreadStats(new SpreadStats())
                .build();
        }
        
        /**
         * Returns a human-readable liquidity assessment.
         */
        public String getLiquidityAssessment() {
            if (liquidityScore >= 80) return "Excellent";
            if (liquidityScore >= 60) return "Good";
            if (liquidityScore >= 40) return "Fair";
            if (liquidityScore >= 20) return "Poor";
            return "Very Poor";
        }
    }
    
    /**
     * Spread statistics container.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpreadStats {
        private int numValidStrikes;
        private double minSpread;
        private double maxSpread;
        private double medianSpread;
        private double meanSpread;
        private int thinStrikesCount;
        private int wideStrikesCount;
        private double thinStrikesPercent;
        
        public SpreadStats() {
            this.minSpread = 0;
            this.maxSpread = 100;
            this.medianSpread = 100;
            this.meanSpread = 100;
        }
    }
}
