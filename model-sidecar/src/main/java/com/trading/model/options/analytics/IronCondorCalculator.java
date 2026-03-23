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
 * Calculates Iron Condor Edge Metrics - fallback when Iron Fly is not viable.
 * 
 * Key differences from Iron Fly:
 * - Separate short call and short put strikes (wider spread)
 * - Wider profit zone but lower max profit
 * - Better for trending or higher volatility environments
 */
public class IronCondorCalculator {
    
    private static final int MAX_WIDEN_ITERATIONS = 3;
    private static final double MIN_PROFIT_ZONE_PCT = 0.04; // Minimum 4% profit zone
    
    /**
     * Calculates Iron Condor metrics as fallback to Iron Fly.
     */
    public IronCondorResult calculateCondor(ExpirationSlice slice, CondorConstructionParams params) {
        if (slice == null || slice.getStrikes() == null || slice.getStrikes().isEmpty()) {
            return IronCondorResult.empty();
        }
        
        IronCondorResult result = new IronCondorResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        // Select short strikes
        CondorStrikes strikes = selectCondorStrikes(slice, params);
        result.setSelectedStrikes(strikes);
        
        if (strikes == null || !strikes.isValid()) {
            result.setStatus(CondorStatus.FAILED_CONSTRUCTION);
            return result;
        }
        
        // Construct the condor
        CondorConstruction construction = constructCondor(slice, strikes, params);
        result.setConstruction(construction);
        
        if (construction == null || !construction.isValid()) {
            result.setStatus(CondorStatus.FAILED_CONSTRUCTION);
            return result;
        }
        
        // Calculate edge metrics
        calculateCondorMetrics(result, slice, construction);
        
        // Calculate conviction score
        result.setConvictionScore(calculateConvictionScore(result));
        result.setUrgency(calculateUrgency(result));
        result.setStatus(CondorStatus.VALID);
        
        return result;
    }
    
    /**
     * Selects optimal short strikes for the condor.
     */
    private CondorStrikes selectCondorStrikes(ExpirationSlice slice, CondorConstructionParams params) {
        double spot = slice.getUnderlyingPrice();
        if (spot == null) return null;
        
        double callDeltaTarget = params.getShortCallDelta() != null ? 
            params.getShortCallDelta() : 0.20;
        double putDeltaTarget = params.getShortPutDelta() != null ? 
            params.getShortPutDelta() : 0.20;
        
        CondorStrikes strikes = new CondorStrikes();
        
        // Find short call strike (higher delta = lower strike for calls)
        strikes.setShortCallStrike(findStrikeByDelta(
            slice.getStrikes(), callDeltaTarget, true, >));
        
        // Find short put strike (higher delta = higher strike for puts)
        strikes.setShortPutStrike(findStrikeByDelta(
            slice.getStrikes(), -putDeltaTarget, false, <));
        
        if (strikes.getShortCallStrike() == null || strikes.getShortPutStrike() == null) {
            return strikes;
        }
        
        // Check profit zone
        double profitZone = strikes.getShortCallStrike().getStrike() - 
                           strikes.getShortPutStrike().getStrike();
        double profitZonePct = profitZone / spot;
        
        strikes.setProfitZoneDollar(profitZone);
        strikes.setProfitZonePercent(profitZonePct);
        strikes.setValid(profitZonePct >= MIN_PROFIT_ZONE_PCT);
        
        // Calculate probabilities
        strikes.setShortCallProbability(getDeltaProbability(strikes.getShortCallStrike().getCallDelta()));
        strikes.setShortPutProbability(getDeltaProbability(strikes.getShortPutStrike().getPutDelta()));
        
        return strikes;
    }
    
    /**
     * Constructs the condor with long wings.
     */
    private CondorConstruction constructCondor(ExpirationSlice slice, CondorStrikes strikes, 
                                             CondorConstructionParams params) {
        CondorConstruction construction = new CondorConstruction();
        construction.setShortCallStrike(strikes.getShortCallStrike());
        construction.setShortPutStrike(strikes.getShortPutStrike());
        
        double spot = slice.getUnderlyingPrice();
        double wingWidthPct = params.getWingWidthPercent() != null ? 
            params.getWingWidthPercent() : 0.05;
        
        // Find long strikes
        double targetLongCallStrike = strikes.getShortCallStrike().getStrike() * (1 + wingWidthPct);
        double targetLongPutStrike = strikes.getShortPutStrike().getStrike() * (1 - wingWidthPct);
        
        OptionStrike longCall = findStrike(slice, targetLongCallStrike);
        OptionStrike longPut = findStrike(slice, targetLongPutStrike);
        
        if (longCall == null || longPut == null) {
            construction.setValid(false);
            return construction;
        }
        
        construction.setLongCallStrike(longCall);
        construction.setLongPutStrike(longPut);
        construction.setValid(true);
        
        // Calculate prices
        double shortCallCredit = strikes.getShortCallStrike().getCallBid();
        double shortPutCredit = strikes.getShortPutStrike().getPutBid();
        double longCallDebit = longCall.getCallAsk();
        double longPutDebit = longPut.getPutAsk();
        
        double netCredit = (shortCallCredit + shortPutCredit) - (longCallDebit + longPutDebit);
        construction.setNetCredit(netCredit);
        
        // Calculate max risk (width of smallest wing)
        double callWingWidth = longCall.getStrike() - strikes.getShortCallStrike().getStrike();
        double putWingWidth = strikes.getShortPutStrike().getStrike() - longPut.getStrike();
        double minWingWidth = Math.min(callWingWidth, putWingWidth);
        
        construction.setCallWingWidth(callWingWidth);
        construction.setPutWingWidth(putWingWidth);
        construction.setMaxRisk(minWingWidth - netCredit);
        
        // Breakevens
        construction.setUpperBreakeven(strikes.getShortCallStrike().getStrike() + netCredit);
        construction.setLowerBreakeven(strikes.getShortPutStrike().getStrike() - netCredit);
        construction.setProfitZoneWidth(strikes.getShortCallStrike().getStrike() - 
                                        strikes.getShortPutStrike().getStrike());
        
        return construction;
    }
    
    /**
     * Calculates condor-specific metrics.
     */
    private void calculateCondorMetrics(IronCondorResult result, ExpirationSlice slice, 
                                       CondorConstruction construction) {
        // Profit zone ratio
        double spot = slice.getUnderlyingPrice();
        double profitZonePct = construction.getProfitZoneWidth() / spot;
        result.setProfitZonePercent(profitZonePct * 100.0);
        
        // Risk-reward ratio
        if (construction.getMaxRisk() > 0) {
            result.setRiskRewardRatio(construction.getNetCredit() / construction.getMaxRisk());
        }
        
        // Probability of profit (simplified)
        double pop = estimateProbabilityOfProfit(slice, construction);
        result.setProbabilityOfProfit(pop);
        
        // Breakeven distance
        result.setUpperBreakevenDistance(construction.getUpperBreakeven() - spot);
        result.setLowerBreakevenDistance(spot - construction.getLowerBreakeven());
        result.setBreakevenDistancePercent(
            Math.min(result.getUpperBreakevenDistance(), result.getLowerBreakevenDistance()) / spot * 100.0);
        
        // Theta efficiency
        double netTheta = calculateNetTheta(construction);
        result.setNetDailyTheta(netTheta);
        result.setThetaEfficiency(netTheta / construction.getNetCredit());
        
        // Wing balance score (0-100)
        double wingBalance = 100 - Math.abs(construction.getCallWingWidth() - construction.getPutWingWidth()) / 
            construction.getProfitZoneWidth() * 100;
        result.setWingBalanceScore(wingBalance);
        
        // Distance score (how far from current price are breakevens)
        double distanceScore = result.getBreakevenDistancePercent() / profitZonePct;
        result.setDistanceScore(Math.min(100, distanceScore * 10));
    }
    
    /**
     * Estimates probability of profit.
     */
    private double estimateProbabilityOfProfit(ExpirationSlice slice, CondorConstruction construction) {
        OptionStrike atm = slice.getAtmStrike();
        double iv = atm != null && atm.getAverageIv() != null ? atm.getAverageIv() : 0.2;
        double t = slice.getDaysToExpiry() / 365.0;
        double spot = slice.getUnderlyingPrice();
        
        // Standard deviation at expiration
        double sd = spot * iv * Math.sqrt(t);
        
        // Distance to breakevens in SD
        double upperDist = (construction.getUpperBreakeven() - spot) / sd;
        double lowerDist = (spot - construction.getLowerBreakeven()) / sd;
        
        // Probability of staying within breakevens
        double probAboveLower = normalCdf(lowerDist);
        double probBelowUpper = normalCdf(upperDist);
        
        return (probBelowUpper - (1 - probAboveLower)) * 100.0;
    }
    
    /**
     * Calculates net daily theta.
     */
    private double calculateNetTheta(CondorConstruction construction) {
        double shortCallTheta = construction.getShortCallStrike().getCallTheta() != null ? 
            construction.getShortCallStrike().getCallTheta() : 0;
        double shortPutTheta = construction.getShortPutStrike().getPutTheta() != null ? 
            construction.getShortPutStrike().getPutTheta() : 0;
        double longCallTheta = construction.getLongCallStrike().getCallTheta() != null ? 
            construction.getLongCallStrike().getCallTheta() : 0;
        double longPutTheta = construction.getLongPutStrike().getPutTheta() != null ? 
            construction.getLongPutStrike().getPutTheta() : 0;
        
        return -(shortCallTheta + shortPutTheta) + (longCallTheta + longPutTheta);
    }
    
    /**
     * Calculates conviction score for the condor.
     */
    private double calculateConvictionScore(IronCondorResult result) {
        double score = 0;
        
        // Profit zone (wider is better, up to a point)
        double profitZoneScore = Math.min(100, result.getProfitZonePercent() * 5);
        score += profitZoneScore * 0.20;
        
        // Risk-reward ratio
        double rrScore = Math.min(100, result.getRiskRewardRatio() * 200);
        score += rrScore * 0.20;
        
        // Probability of profit
        double popScore = result.getProbabilityOfProfit();
        score += popScore * 0.25;
        
        // Breakeven distance
        double distanceScore = result.getDistanceScore();
        score += distanceScore * 0.15;
        
        // Wing balance
        score += result.getWingBalanceScore() * 0.10;
        
        // Theta efficiency
        double thetaScore = Math.min(100, result.getThetaEfficiency() * 100);
        score += thetaScore * 0.10;
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculates urgency level.
     */
    private CondorUrgency calculateUrgency(IronCondorResult result) {
        double conviction = result.getConvictionScore();
        double pop = result.getProbabilityOfProfit();
        
        if (conviction > 70 && pop > 60) {
            return CondorUrgency.HIGH;
        }
        if (conviction > 55) {
            return CondorUrgency.NORMAL;
        }
        if (conviction > 40) {
            return CondorUrgency.LOW;
        }
        return CondorUrgency.WATCH;
    }
    
    // Helper methods
    
    private OptionStrike findStrikeByDelta(List<OptionStrike> strikes, double targetDelta, 
                                          boolean isCall, java.util.function.BiPredicate<Double, Double> comparison) {
        OptionStrike best = null;
        double bestDiff = Double.MAX_VALUE;
        
        for (OptionStrike strike : strikes) {
            Double delta = isCall ? strike.getCallDelta() : strike.getPutDelta();
            if (delta == null) continue;
            
            double diff = Math.abs(delta - targetDelta);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = strike;
            }
        }
        
        return best;
    }
    
    private OptionStrike findStrike(ExpirationSlice slice, double targetStrike) {
        return slice.getStrikes().stream()
            .min(Comparator.comparing(s -> Math.abs(s.getStrike() - targetStrike)))
            .orElse(null);
    }
    
    private double getDeltaProbability(Double delta) {
        if (delta == null) return 0.5;
        return Math.abs(delta) * 100.0;
    }
    
    private double normalCdf(double x) {
        return 0.5 * (1.0 + Math.tanh(x * Math.sqrt(2.0 / Math.PI)));
    }
    
    // Result classes
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IronCondorResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private CondorStrikes selectedStrikes;
        private CondorConstruction construction;
        private double profitZonePercent;
        private double riskRewardRatio;
        private double probabilityOfProfit;
        private double upperBreakevenDistance;
        private double lowerBreakevenDistance;
        private double breakevenDistancePercent;
        private double netDailyTheta;
        private double thetaEfficiency;
        private double wingBalanceScore;
        private double distanceScore;
        private double convictionScore;
        private CondorUrgency urgency;
        private CondorStatus status;
        
        public static IronCondorResult empty() {
            return IronCondorResult.builder()
                .status(CondorStatus.NO_STRIKES)
                .urgency(CondorUrgency.WATCH)
                .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondorStrikes {
        private OptionStrike shortCallStrike;
        private OptionStrike shortPutStrike;
        private double profitZoneDollar;
        private double profitZonePercent;
        private double shortCallProbability;
        private double shortPutProbability;
        private boolean valid;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondorConstruction {
        private OptionStrike shortCallStrike;
        private OptionStrike shortPutStrike;
        private OptionStrike longCallStrike;
        private OptionStrike longPutStrike;
        private double callWingWidth;
        private double putWingWidth;
        private double profitZoneWidth;
        private double netCredit;
        private double maxRisk;
        private double upperBreakeven;
        private double lowerBreakeven;
        private boolean valid;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondorConstructionParams {
        private Double shortCallDelta;
        private Double shortPutDelta;
        private Double wingWidthPercent;
    }
    
    public enum CondorStatus {
        VALID,
        NO_STRIKES,
        FAILED_CONSTRUCTION
    }
    
    public enum CondorUrgency {
        HIGH,
        NORMAL,
        LOW,
        WATCH
    }
}
