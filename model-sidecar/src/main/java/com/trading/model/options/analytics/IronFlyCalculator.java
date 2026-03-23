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
 * Calculates Iron-Fly Edge Metrics for options trade construction.
 * 
 * Key calculations:
 * - FlyCenterScore (PinIndex + proximity + Gamma Tilt + OI surge)
 * - WingRichnessScore
 * - Theta-to-Move (T/M)
 * - BEPG (breach probability)
 * - CpR (Credit-per-Risk)
 * - MRB (Magnet-Risk Balance)
 */
public class IronFlyCalculator {
    
    // Constants for scoring
    private static final double MAX_WING_WIDTH_PCT = 0.10;  // 10% max wing width
    private static final int MAX_WIDEN_ITERATIONS = 3;
    
    /**
     * Calculates Iron-Fly edge metrics for a potential fly construction.
     */
    public IronFlyResult calculateFly(ExpirationSlice slice, FlyConstructionParams params) {
        if (slice == null || slice.getStrikes() == null || slice.getStrikes().isEmpty()) {
            return IronFlyResult.empty();
        }
        
        IronFlyResult result = new IronFlyResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        // Select fly center
        FlyCenter center = selectFlyCenter(slice, params);
        result.setCenter(center);
        
        if (center == null) {
            return result;
        }
        
        // Determine wing width
        double wingWidth = params.getInitialWingWidthPercent() != null ? 
            params.getInitialWingWidthPercent() : 0.05; // Default 5%
        
        // Try to construct the fly with potential widening
        FlyConstruction construction = constructFly(slice, center, wingWidth, params);
        result.setConstruction(construction);
        
        if (construction == null || !construction.isValid()) {
            result.setStatus(FlyStatus.FAILED_CONSTRUCTION);
            return result;
        }
        
        // Calculate metrics
        calculateFlyCenterScore(result, slice, center, construction);
        calculateWingRichnessScore(result, slice, construction);
        calculateThetaToMove(result, slice, construction);
        calculateBepg(result, slice, construction);
        calculateCreditPerRisk(result, construction);
        calculateMagnetRiskBalance(result, slice, center, construction);
        
        // Calculate composite conviction score
        result.setConvictionScore(calculateConvictionScore(result));
        
        // Determine urgency
        result.setUrgency(calculateUrgency(result, slice));
        
        result.setStatus(FlyStatus.VALID);
        
        return result;
    }
    
    /**
     * Selects the optimal fly center strike.
     */
    private FlyCenter selectFlyCenter(ExpirationSlice slice, FlyConstructionParams params) {
        double targetPrice = params.getTargetCenterPrice() != null ? 
            params.getTargetCenterPrice() : slice.getUnderlyingPrice();
        
        if (targetPrice == null) return null;
        
        // Find strike closest to target
        OptionStrike centerStrike = slice.getStrikes().stream()
            .min(Comparator.comparing(s -> Math.abs(s.getStrike() - targetPrice)))
            .orElse(null);
        
        if (centerStrike == null) return null;
        
        FlyCenter center = new FlyCenter();
        center.setStrike(centerStrike.getStrike());
        center.setDistanceFromSpot(Math.abs(centerStrike.getStrike() - slice.getUnderlyingPrice()));
        center.setDistancePercent(center.getDistanceFromSpot() / slice.getUnderlyingPrice());
        center.setPinRiskScore(calculatePinRiskScore(slice, centerStrike));
        center.setGammaTiltScore(calculateGammaTiltScore(slice, centerStrike));
        center.setOiSurgeScore(calculateOiSurgeScore(slice, centerStrike));
        
        // Combined center score
        center.setCenterScore(
            center.getPinRiskScore() * 0.4 +
            center.getGammaTiltScore() * 0.3 +
            center.getOiSurgeScore() * 0.3
        );
        
        return center;
    }
    
    /**
     * Constructs the fly with wing-widening loop.
     */
    private FlyConstruction constructFly(ExpirationSlice slice, FlyCenter center, 
                                       double initialWingWidth, FlyConstructionParams params) {
        FlyConstruction construction = new FlyConstruction();
        construction.setCenterStrike(center.getStrike());
        
        double currentWidth = initialWingWidth;
        int iteration = 0;
        
        while (iteration < MAX_WIDEN_ITERATIONS) {
            // Calculate wing strikes
            double wingWidthDollar = slice.getUnderlyingPrice() * currentWidth;
            double shortCallStrike = center.getStrike() + wingWidthDollar;
            double shortPutStrike = center.getStrike() - wingWidthDollar;
            double longCallStrike = center.getStrike() + wingWidthDollar * 2;
            double longPutStrike = center.getStrike() - wingWidthDollar * 2;
            
            // Find strikes
            OptionStrike shortCall = findStrike(slice, shortCallStrike);
            OptionStrike shortPut = findStrike(slice, shortPutStrike);
            OptionStrike longCall = findStrike(slice, longCallStrike);
            OptionStrike longPut = findStrike(slice, longPutStrike);
            
            if (shortCall != null && shortPut != null && 
                longCall != null && longPut != null) {
                
                // Check if strikes are valid (not thin, have valid prices)
                if (areStrikesValid(shortCall, shortPut, longCall, longPut)) {
                    construction.setShortCallStrike(shortCall);
                    construction.setShortPutStrike(shortPut);
                    construction.setLongCallStrike(longCall);
                    construction.setLongPutStrike(longPut);
                    construction.setWingWidthPercent(currentWidth);
                    construction.setWingWidthDollar(wingWidthDollar);
                    construction.setValid(true);
                    construction.setWidened(iteration > 0);
                    construction.setWidenIterations(iteration);
                    
                    // Calculate prices
                    calculateFlyPrices(construction);
                    
                    return construction;
                }
            }
            
            // Widen wings for next iteration
            currentWidth += params.getWidenIncrementPercent() != null ? 
                params.getWidenIncrementPercent() : 0.01;
            iteration++;
            
            // Check max width
            if (currentWidth > MAX_WING_WIDTH_PCT) {
                break;
            }
        }
        
        // Failed to construct with widening
        construction.setValid(false);
        return construction;
    }
    
    /**
     * Calculates the fly center score.
     */
    private void calculateFlyCenterScore(IronFlyResult result, ExpirationSlice slice, 
                                        FlyCenter center, FlyConstruction construction) {
        // PinIndex contribution (0-100)
        double pinScore = center.getPinRiskScore();
        
        // Proximity to forward price (0-100, higher = closer)
        double proximityScore = Math.max(0, 100 - center.getDistancePercent() * 1000);
        
        // Gamma tilt (-100 to 100, normalized)
        double gammaTiltScore = center.getGammaTiltScore();
        
        // OI surge score
        double oiSurgeScore = center.getOiSurgeScore();
        
        // Combined center score
        double centerScore = (pinScore * 0.3 + proximityScore * 0.3 + 
                             gammaTiltScore * 0.2 + oiSurgeScore * 0.2);
        
        result.setFlyCenterScore(centerScore);
    }
    
    /**
     * Calculates wing richness score.
     */
    private void calculateWingRichnessScore(IronFlyResult result, ExpirationSlice slice, 
                                            FlyConstruction construction) {
        OptionStrike atm = slice.getAtmStrike();
        if (atm == null) return;
        
        double atmIv = atm.getAverageIv() != null ? atm.getAverageIv() : 0.2;
        
        // Compare wing IV to ATM IV
        double shortCallIv = construction.getShortCallStrike().getCallIv();
        double shortPutIv = construction.getShortPutStrike().getPutIv();
        
        double avgWingIv = (shortCallIv + shortPutIv) / 2.0;
        
        // Wing richness: how much premium do we collect vs ATM?
        double richness = (avgWingIv - atmIv) / atmIv * 100.0;
        
        // Score: positive richness is good for selling (sellers collect more)
        double score = Math.min(100, Math.max(0, 50 + richness * 2));
        
        result.setWingRichnessScore(score);
        result.setWingRichnessPercent(richness);
    }
    
    /**
     * Calculates Theta-to-Move ratio.
     */
    private void calculateThetaToMove(IronFlyResult result, ExpirationSlice slice, 
                                     FlyConstruction construction) {
        // Calculate daily theta
        double shortCallTheta = construction.getShortCallStrike().getCallTheta() != null ? 
            construction.getShortCallStrike().getCallTheta() : 0;
        double shortPutTheta = construction.getShortPutStrike().getPutTheta() != null ? 
            construction.getShortPutStrike().getPutTheta() : 0;
        double longCallTheta = construction.getLongCallStrike().getCallTheta() != null ? 
            construction.getLongCallStrike().getCallTheta() : 0;
        double longPutTheta = construction.getLongPutStrike().getPutTheta() != null ? 
            construction.getLongPutStrike().getPutTheta() : 0;
        
        double netTheta = -(shortCallTheta + shortPutTheta) + (longCallTheta + longPutTheta);
        
        // Estimate daily expected move
        OptionStrike atm = slice.getAtmStrike();
        double atmIv = atm != null && atm.getAverageIv() != null ? atm.getAverageIv() : 0.2;
        double dailyMove = slice.getUnderlyingPrice() * atmIv / Math.sqrt(slice.getDaysToExpiry());
        
        double tm = dailyMove > 0 ? netTheta / dailyMove : 0;
        
        result.setThetaToMove(tm);
        result.setNetDailyTheta(netTheta);
    }
    
    /**
     * Calculates BEPG (breach probability) - probability of breaching short strikes.
     */
    private void calculateBepg(IronFlyResult result, ExpirationSlice slice, 
                              FlyConstruction construction) {
        // Simplified breach probability using Black-Scholes-like approach
        double spot = slice.getUnderlyingPrice();
        OptionStrike atm = slice.getAtmStrike();
        double iv = atm != null && atm.getAverageIv() != null ? atm.getAverageIv() : 0.2;
        double t = slice.getDaysToExpiry() / 365.0;
        
        // Distance to short strikes in standard deviations
        double callDistance = (construction.getShortCallStrike().getStrike() - spot) / 
            (spot * iv * Math.sqrt(t));
        double putDistance = (spot - construction.getShortPutStrike().getStrike()) / 
            (spot * iv * Math.sqrt(t));
        
        // Approximate probability using normal distribution
        double callBreachProb = 1 - normalCdf(callDistance);
        double putBreachProb = 1 - normalCdf(putDistance);
        
        double totalBreachProb = callBreachProb + putBreachProb;
        
        result.setBepg(totalBreachProb);
        result.setBepgPercent(totalBreachProb * 100.0);
    }
    
    /**
     * Calculates Credit-per-Risk (CpR).
     */
    private void calculateCreditPerRisk(IronFlyResult result, FlyConstruction construction) {
        double credit = construction.getNetCredit();
        double maxRisk = construction.getMaxRisk();
        
        if (maxRisk > 0) {
            result.setCreditPerRisk(credit / maxRisk);
        } else {
            result.setCreditPerRisk(0);
        }
        
        result.setNetCredit(credit);
        result.setMaxRisk(maxRisk);
    }
    
    /**
     * Calculates MRB (Magnet-Risk Balance).
     */
    private void calculateMagnetRiskBalance(IronFlyResult result, ExpirationSlice slice, 
                                          FlyCenter center, FlyConstruction construction) {
        // Magnet strength: how strong is the center strike as a magnet?
        double magnetStrength = center.getPinRiskScore();
        
        // Risk: probability weighted by loss magnitude
        double riskScore = result.getBepg() * 100;
        
        // Balance: magnet should be strong relative to risk
        double mrb = magnetStrength / (riskScore + 1);
        
        result.setMagnetRiskBalance(mrb);
    }
    
    /**
     * Calculates composite conviction score.
     */
    private double calculateConvictionScore(IronFlyResult result) {
        double score = 0;
        
        // Center score (0-100)
        score += result.getFlyCenterScore() * 0.25;
        
        // Wing richness (0-100)
        score += result.getWingRichnessScore() * 0.20;
        
        // Theta-to-Move (normalize to 0-100, higher is better)
        double tmScore = Math.min(100, result.getThetaToMove() * 50);
        score += tmScore * 0.20;
        
        // BEPG (lower is better, invert)
        double bepgScore = Math.max(0, 100 - result.getBepgPercent());
        score += bepgScore * 0.15;
        
        // CpR (higher is better)
        double cprScore = Math.min(100, result.getCreditPerRisk() * 100);
        score += cprScore * 0.10;
        
        // MRB (higher is better)
        double mrbScore = Math.min(100, result.getMagnetRiskBalance() * 20);
        score += mrbScore * 0.10;
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculates urgency level for the trade.
     */
    private FlyUrgency calculateUrgency(IronFlyResult result, ExpirationSlice slice) {
        double conviction = result.getConvictionScore();
        double flipDistance = result.getFlipDistancePercent() != null ? 
            result.getFlipDistancePercent() : 50;
        double wingRichness = result.getWingRichnessPercent();
        
        // High conviction + close to flip + rich wings = URGENT
        if (conviction > 75 && flipDistance < 2 && wingRichness > 5) {
            return FlyUrgency.URGENT;
        }
        
        // High conviction + moderate conditions = HIGH
        if (conviction > 65) {
            return FlyUrgency.HIGH;
        }
        
        // Medium conviction = NORMAL
        if (conviction > 50) {
            return FlyUrgency.NORMAL;
        }
        
        // Low conviction or far from flip = LOW
        if (conviction > 35) {
            return FlyUrgency.LOW;
        }
        
        return FlyUrgency.WATCH;
    }
    
    // Helper methods
    
    private double calculatePinRiskScore(ExpirationSlice slice, OptionStrike strike) {
        // Stub - would calculate actual pin risk
        return 50.0;
    }
    
    private double calculateGammaTiltScore(ExpirationSlice slice, OptionStrike strike) {
        // Stub - would calculate gamma tilt at strike
        return 50.0;
    }
    
    private double calculateOiSurgeScore(ExpirationSlice slice, OptionStrike strike) {
        // Stub - would compare current OI to historical
        return 50.0;
    }
    
    private OptionStrike findStrike(ExpirationSlice slice, double targetStrike) {
        return slice.getStrikes().stream()
            .min(Comparator.comparing(s -> Math.abs(s.getStrike() - targetStrike)))
            .orElse(null);
    }
    
    private boolean areStrikesValid(OptionStrike... strikes) {
        for (OptionStrike s : strikes) {
            if (s == null) return false;
            if (s.isThin()) return false;
        }
        return true;
    }
    
    private void calculateFlyPrices(FlyConstruction construction) {
        // Calculate net credit received
        double shortCallCredit = construction.getShortCallStrike().getCallBid();
        double shortPutCredit = construction.getShortPutStrike().getPutBid();
        double longCallDebit = construction.getLongCallStrike().getCallAsk();
        double longPutDebit = construction.getLongPutStrike().getPutAsk();
        
        double netCredit = (shortCallCredit + shortPutCredit) - (longCallDebit + longPutDebit);
        construction.setNetCredit(netCredit);
        
        // Max risk is wing width minus credit
        double maxRisk = construction.getWingWidthDollar() - netCredit;
        construction.setMaxRisk(Math.max(0, maxRisk));
        
        // Breakevens
        construction.setUpperBreakeven(construction.getCenterStrike() + construction.getWingWidthDollar() - netCredit);
        construction.setLowerBreakeven(construction.getCenterStrike() - construction.getWingWidthDollar() + netCredit);
    }
    
    private double normalCdf(double x) {
        // Approximation of normal CDF
        return 0.5 * (1.0 + Math.tanh(x * Math.sqrt(2.0 / Math.PI)));
    }
    
    // Result classes
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IronFlyResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private FlyCenter center;
        private FlyConstruction construction;
        private double flyCenterScore;
        private double wingRichnessScore;
        private double wingRichnessPercent;
        private double thetaToMove;
        private double netDailyTheta;
        private double bepg;
        private double bepgPercent;
        private double creditPerRisk;
        private double netCredit;
        private double maxRisk;
        private double magnetRiskBalance;
        private double convictionScore;
        private FlyUrgency urgency;
        private FlyStatus status;
        private Double flipDistancePercent;
        
        public static IronFlyResult empty() {
            return IronFlyResult.builder()
                .status(FlyStatus.NO_CENTER)
                .urgency(FlyUrgency.WATCH)
                .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlyCenter {
        private double strike;
        private double distanceFromSpot;
        private double distancePercent;
        private double pinRiskScore;
        private double gammaTiltScore;
        private double oiSurgeScore;
        private double centerScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlyConstruction {
        private double centerStrike;
        private OptionStrike shortCallStrike;
        private OptionStrike shortPutStrike;
        private OptionStrike longCallStrike;
        private OptionStrike longPutStrike;
        private double wingWidthPercent;
        private double wingWidthDollar;
        private double netCredit;
        private double maxRisk;
        private double upperBreakeven;
        private double lowerBreakeven;
        private boolean valid;
        private boolean widened;
        private int widenIterations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlyConstructionParams {
        private Double targetCenterPrice;
        private Double initialWingWidthPercent;
        private Double widenIncrementPercent;
    }
    
    public enum FlyStatus {
        VALID,
        NO_CENTER,
        FAILED_CONSTRUCTION,
        FALLBACK_TO_CONDOR
    }
    
    public enum FlyUrgency {
        URGENT,
        HIGH,
        NORMAL,
        LOW,
        WATCH
    }
}
