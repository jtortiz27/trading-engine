package com.trading.model.options.analytics;

import com.trading.model.options.analytics.IronFlyCalculator.FlyStatus;
import com.trading.model.options.analytics.IronFlyCalculator.FlyUrgency;
import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionsChainData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ranks and prioritizes Iron Fly trade opportunities.
 * 
 * Implements:
 * - FlyConviction composite score
 * - Urgency ranking (flip proximity, wing richness, etc.)
 * - Priority queue for trade ideas
 */
public class FlyRankingEngine {
    
    private static final int MAX_RANKED_FLIES = 10;
    
    private final GammaDeltaFlowCalculator gexCalculator;
    private final OpenInterestCalculator oiCalculator;
    private final VolSurfaceCalculator volCalculator;
    private final RealizedVolCalculator rvCalculator;
    private final LiquidityCalculator liquidityCalculator;
    private final IronFlyCalculator flyCalculator;
    private final IronCondorCalculator condorCalculator;
    
    public FlyRankingEngine() {
        this.gexCalculator = new GammaDeltaFlowCalculator();
        this.oiCalculator = new OpenInterestCalculator();
        this.volCalculator = new VolSurfaceCalculator();
        this.rvCalculator = new RealizedVolCalculator();
        this.liquidityCalculator = new LiquidityCalculator();
        this.flyCalculator = new IronFlyCalculator();
        this.condorCalculator = new IronCondorCalculator();
    }
    
    /**
     * Generates ranked trade ideas from options chain data.
     */
    public RankingResult generateTradeIdeas(OptionsChainData chainData) {
        if (chainData == null || chainData.getExpirations() == null || 
            chainData.getExpirations().isEmpty()) {
            return RankingResult.empty();
        }
        
        RankingResult result = new RankingResult();
        result.setUnderlying(chainData.getUnderlyingSymbol());
        result.setSpot(chainData.getUnderlyingPrice());
        result.setTimestamp(chainData.getTimestamp());
        
        List<FlyRankingEntry> entries = new ArrayList<>();
        
        // Analyze each expiration
        for (ExpirationSlice slice : chainData.getExpirations()) {
            // Calculate base metrics
            var gexResult = gexCalculator.calculate(slice);
            var oiResult = oiCalculator.calculate(slice);
            var volResult = volCalculator.calculate(slice);
            var liquidityResult = liquidityCalculator.calculate(slice);
            
            // Skip if too thin
            if (liquidityResult.isThin()) {
                continue;
            }
            
            // Try Iron Fly first
            var flyParams = IronFlyCalculator.FlyConstructionParams.builder()
                .targetCenterPrice(chainData.getUnderlyingPrice())
                .initialWingWidthPercent(0.05)
                .widenIncrementPercent(0.02)
                .build();
            
            var flyResult = flyCalculator.calculateFly(slice, flyParams);
            
            if (flyResult.getStatus() == FlyStatus.VALID) {
                FlyRankingEntry entry = createFlyEntry(flyResult, gexResult, oiResult, 
                                                       volResult, liquidityResult);
                entries.add(entry);
            } else {
                // Fallback to Iron Condor
                var condorParams = IronCondorCalculator.CondorConstructionParams.builder()
                    .shortCallDelta(0.20)
                    .shortPutDelta(0.20)
                    .wingWidthPercent(0.05)
                    .build();
                
                var condorResult = condorCalculator.calculateCondor(slice, condorParams);
                
                if (condorResult.getStatus() == IronCondorCalculator.CondorStatus.VALID) {
                    FlyRankingEntry entry = createCondorEntry(condorResult, gexResult, oiResult,
                                                              volResult, liquidityResult);
                    entries.add(entry);
                }
            }
        }
        
        // Rank and filter
        List<FlyRankingEntry> ranked = rankEntries(entries);
        result.setRankedOpportunities(ranked);
        result.setTopOpportunity(ranked.isEmpty() ? null : ranked.get(0));
        
        // Generate summary
        result.setSummary(generateSummary(result));
        
        return result;
    }
    
    /**
     * Creates a ranking entry from Iron Fly result.
     */
    private FlyRankingEntry createFlyEntry(IronFlyCalculator.IronFlyResult flyResult,
                                          GammaDeltaFlowCalculator.GammaDeltaFlowResult gexResult,
                                          OpenInterestCalculator.OpenInterestResult oiResult,
                                          VolSurfaceCalculator.VolSurfaceResult volResult,
                                          LiquidityCalculator.LiquidityResult liquidityResult) {
        FlyRankingEntry entry = new FlyRankingEntry();
        
        entry.setStrategyType(StrategyType.IRON_FLY);
        entry.setExpiryDate(flyResult.getExpiryDate());
        entry.setDaysToExpiry(flyResult.getDaysToExpiry());
        entry.setUnderlyingPrice(flyResult.getUnderlyingPrice());
        
        // Strikes
        if (flyResult.getConstruction() != null) {
            entry.setCenterStrike(flyResult.getConstruction().getCenterStrike());
            entry.setShortCallStrike(flyResult.getConstruction().getShortCallStrike() != null ? 
                flyResult.getConstruction().getShortCallStrike().getStrike() : null);
            entry.setShortPutStrike(flyResult.getConstruction().getShortPutStrike() != null ? 
                flyResult.getConstruction().getShortPutStrike().getStrike() : null);
            entry.setLongCallStrike(flyResult.getConstruction().getLongCallStrike() != null ? 
                flyResult.getConstruction().getLongCallStrike().getStrike() : null);
            entry.setLongPutStrike(flyResult.getConstruction().getLongPutStrike() != null ? 
                flyResult.getConstruction().getLongPutStrike().getStrike() : null);
            entry.setWingWidth(flyResult.getConstruction().getWingWidthDollar());
        }
        
        // Metrics
        entry.setConvictionScore(flyResult.getConvictionScore());
        entry.setUrgency(flyResult.getUrgency());
        entry.setNetCredit(flyResult.getNetCredit());
        entry.setMaxRisk(flyResult.getMaxRisk());
        entry.setCreditPerRisk(flyResult.getCreditPerRisk());
        entry.setBepg(flyResult.getBepgPercent());
        entry.setThetaToMove(flyResult.getThetaToMove());
        entry.setFlyCenterScore(flyResult.getFlyCenterScore());
        entry.setWingRichnessScore(flyResult.getWingRichnessScore());
        
        // GEX context
        entry.setFlipDistancePercent(gexResult.getFlipDistancePercent());
        entry.setAtmGammaTilt(gexResult.getAtmGammaTilt());
        entry.setNearestFlipLevel(gexResult.getNearestFlipLevel());
        
        // OI context
        entry.setPinRiskIndex(oiResult.getPinRiskIndex());
        entry.setMaxPainStrike(oiResult.getMaxPainStrike());
        entry.setMaxPainDistance(oiResult.getMaxPainDistance());
        
        // Vol context
        entry.setRiskReversal25(volResult.getRiskReversal25());
        entry.setButterfly25(volResult.getButterfly25());
        
        // Liquidity
        entry.setLiquidityScore(liquidityResult.getLiquidityScore());
        entry.setThin(liquidityResult.isThin());
        
        // Composite ranking score
        entry.setCompositeScore(calculateCompositeScore(entry));
        
        return entry;
    }
    
    /**
     * Creates a ranking entry from Iron Condor result.
     */
    private FlyRankingEntry createCondorEntry(IronCondorCalculator.IronCondorResult condorResult,
                                             GammaDeltaFlowCalculator.GammaDeltaFlowResult gexResult,
                                             OpenInterestCalculator.OpenInterestResult oiResult,
                                             VolSurfaceCalculator.VolSurfaceResult volResult,
                                             LiquidityCalculator.LiquidityResult liquidityResult) {
        FlyRankingEntry entry = new FlyRankingEntry();
        
        entry.setStrategyType(StrategyType.IRON_CONDOR);
        entry.setExpiryDate(condorResult.getExpiryDate());
        entry.setDaysToExpiry(condorResult.getDaysToExpiry());
        entry.setUnderlyingPrice(condorResult.getUnderlyingPrice());
        
        // Strikes
        if (condorResult.getConstruction() != null) {
            entry.setShortCallStrike(condorResult.getConstruction().getShortCallStrike() != null ? 
                condorResult.getConstruction().getShortCallStrike().getStrike() : null);
            entry.setShortPutStrike(condorResult.getConstruction().getShortPutStrike() != null ? 
                condorResult.getConstruction().getShortPutStrike().getStrike() : null);
            entry.setLongCallStrike(condorResult.getConstruction().getLongCallStrike() != null ? 
                condorResult.getConstruction().getLongCallStrike().getStrike() : null);
            entry.setLongPutStrike(condorResult.getConstruction().getLongPutStrike() != null ? 
                condorResult.getConstruction().getLongPutStrike().getStrike() : null);
            entry.setWingWidth(condorResult.getConstruction().getCallWingWidth());
        }
        
        // Metrics
        entry.setConvictionScore(condorResult.getConvictionScore());
        entry.setCondorUrgency(condorResult.getUrgency());
        entry.setNetCredit(condorResult.getConstruction() != null ? 
            condorResult.getConstruction().getNetCredit() : 0);
        entry.setMaxRisk(condorResult.getConstruction() != null ? 
            condorResult.getConstruction().getMaxRisk() : 0);
        entry.setRiskRewardRatio(condorResult.getRiskRewardRatio());
        entry.setProbabilityOfProfit(condorResult.getProbabilityOfProfit());
        entry.setProfitZonePercent(condorResult.getProfitZonePercent());
        entry.setNetDailyTheta(condorResult.getNetDailyTheta());
        
        // GEX context
        entry.setFlipDistancePercent(gexResult.getFlipDistancePercent());
        entry.setAtmGammaTilt(gexResult.getAtmGammaTilt());
        
        // OI context
        entry.setPinRiskIndex(oiResult.getPinRiskIndex());
        
        // Vol context
        entry.setRiskReversal25(volResult.getRiskReversal25());
        
        // Liquidity
        entry.setLiquidityScore(liquidityResult.getLiquidityScore());
        entry.setThin(liquidityResult.isThin());
        
        // Composite ranking score (slightly lower base score for condors)
        entry.setCompositeScore(calculateCompositeScore(entry) * 0.95);
        
        return entry;
    }
    
    /**
     * Ranks entries by composite score and urgency.
     */
    private List<FlyRankingEntry> rankEntries(List<FlyRankingEntry> entries) {
        // Sort by composite score (descending)
        entries.sort(Comparator.comparing(FlyRankingEntry::getCompositeScore).reversed());
        
        // Take top N
        return entries.stream()
            .limit(MAX_RANKED_FLIES)
            .peek(e -> e.setRank(entries.indexOf(e) + 1))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates composite score for ranking.
     */
    private double calculateCompositeScore(FlyRankingEntry entry) {
        double score = entry.getConvictionScore();
        
        // Bonus for high liquidity
        score += entry.getLiquidityScore() * 0.1;
        
        // Bonus for close flip level (pin potential)
        if (entry.getFlipDistancePercent() != null && entry.getFlipDistancePercent() < 2.0) {
            score += 10;
        }
        
        // Penalty for far max pain
        if (entry.getMaxPainDistance() != null) {
            double painPenalty = Math.min(15, entry.getMaxPainDistance() / entry.getUnderlyingPrice() * 100);
            score -= painPenalty;
        }
        
        // Strategy preference
        if (entry.getStrategyType() == StrategyType.IRON_FLY) {
            score += 5; // Slight preference for flies
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Generates summary statistics.
     */
    private RankingSummary generateSummary(RankingResult result) {
        RankingSummary summary = new RankingSummary();
        
        List<FlyRankingEntry> opportunities = result.getRankedOpportunities();
        
        summary.setTotalOpportunities(opportunities.size());
        summary.setIronFlies((int) opportunities.stream()
            .filter(e -> e.getStrategyType() == StrategyType.IRON_FLY).count());
        summary.setIronCondors((int) opportunities.stream()
            .filter(e -> e.getStrategyType() == StrategyType.IRON_CONDOR).count());
        
        long urgentCount = opportunities.stream()
            .filter(e -> e.getUrgency() == FlyUrgency.URGENT || 
                       e.getCondorUrgency() == IronCondorCalculator.CondorUrgency.HIGH)
            .count();
        summary.setUrgentOpportunities((int) urgentCount);
        
        if (!opportunities.isEmpty()) {
            double avgConviction = opportunities.stream()
                .mapToDouble(FlyRankingEntry::getConvictionScore)
                .average().orElse(0);
            summary.setAverageConviction(avgConviction);
            
            double bestComposite = opportunities.stream()
                .mapToDouble(FlyRankingEntry::getCompositeScore)
                .max().orElse(0);
            summary.setBestCompositeScore(bestComposite);
        }
        
        return summary;
    }
    
    // Result classes
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingResult {
        private String underlying;
        private Double spot;
        private java.time.LocalDate timestamp;
        private List<FlyRankingEntry> rankedOpportunities;
        private FlyRankingEntry topOpportunity;
        private RankingSummary summary;
        
        public static RankingResult empty() {
            return RankingResult.builder()
                .rankedOpportunities(new ArrayList<>())
                .summary(new RankingSummary())
                .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlyRankingEntry {
        private int rank;
        private StrategyType strategyType;
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        
        // Strikes
        private Double centerStrike;
        private Double shortCallStrike;
        private Double shortPutStrike;
        private Double longCallStrike;
        private Double longPutStrike;
        private Double wingWidth;
        
        // Scores
        private double convictionScore;
        private FlyUrgency urgency;
        private IronCondorCalculator.CondorUrgency condorUrgency;
        private double compositeScore;
        
        // P&L
        private double netCredit;
        private double maxRisk;
        private double creditPerRisk;
        private double riskRewardRatio;
        
        // Risk metrics
        private double bepg;
        private double probabilityOfProfit;
        private double profitZonePercent;
        
        // Greek metrics
        private double thetaToMove;
        private double netDailyTheta;
        
        // Edge metrics
        private double flyCenterScore;
        private double wingRichnessScore;
        
        // Context
        private Double flipDistancePercent;
        private double atmGammaTilt;
        private double nearestFlipLevel;
        private double pinRiskIndex;
        private double maxPainStrike;
        private Double maxPainDistance;
        private Double riskReversal25;
        private Double butterfly25;
        private int liquidityScore;
        private boolean thin;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingSummary {
        private int totalOpportunities;
        private int ironFlies;
        private int ironCondors;
        private int urgentOpportunities;
        private double averageConviction;
        private double bestCompositeScore;
    }
    
    public enum StrategyType {
        IRON_FLY,
        IRON_CONDOR
    }
}
