package com.trading.model.options.analytics;

import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionStrike;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Calculates Open Interest Structure metrics for options analysis.
 * 
 * Key calculations:
 * - OI Walls (OI >= 5x rolling 5-strike SMA)
 * - OI Momentum (day-over-day %)
 * - Pin-Risk Index (PinIndex)
 * - Wall distance ($ and %)
 */
public class OpenInterestCalculator {
    
    private static final double OI_WALL_MULTIPLIER = 5.0;
    private static final int ROLLING_WINDOW = 5;
    
    /**
     * Calculates OI structure metrics for an expiration slice.
     */
    public OpenInterestResult calculate(ExpirationSlice slice) {
        if (slice == null || slice.getStrikes() == null || slice.getStrikes().isEmpty()) {
            return OpenInterestResult.empty();
        }
        
        OpenInterestResult result = new OpenInterestResult();
        result.setExpiryDate(slice.getExpiryDate());
        result.setDaysToExpiry(slice.getDaysToExpiry());
        result.setUnderlyingPrice(slice.getUnderlyingPrice());
        
        List<OptionStrike> strikes = new ArrayList<>(slice.getStrikes());
        strikes.sort(Comparator.comparing(OptionStrike::getStrike));
        
        // Calculate OI walls
        List<OiWall> walls = findOiWalls(strikes);
        result.setOiWalls(walls);
        result.setNumWalls(walls.size());
        
        // Find nearest wall
        OiWall nearestWall = findNearestWall(walls, slice.getUnderlyingPrice());
        result.setNearestWall(nearestWall);
        
        if (nearestWall != null && slice.getUnderlyingPrice() != null) {
            double distance = Math.abs(nearestWall.getStrike() - slice.getUnderlyingPrice());
            result.setNearestWallDistanceDollar(distance);
            result.setNearestWallDistancePercent(
                distance / slice.getUnderlyingPrice() * 100.0);
        }
        
        // Calculate Pin Risk Index
        double pinIndex = calculatePinRiskIndex(strikes, slice.getUnderlyingPrice());
        result.setPinRiskIndex(pinIndex);
        
        // Calculate OI concentration
        double concentration = calculateOiConcentration(strikes);
        result.setOiConcentration(concentration);
        
        // Calculate call/put wall distribution
        WallDistribution dist = calculateWallDistribution(walls);
        result.setWallDistribution(dist);
        
        // Find max pain
        double maxPain = findMaxPain(strikes);
        result.setMaxPainStrike(maxPain);
        if (slice.getUnderlyingPrice() != null) {
            result.setMaxPainDistance(Math.abs(maxPain - slice.getUnderlyingPrice()));
        }
        
        return result;
    }
    
    /**
     * Finds OI walls - strikes with OI >= 5x rolling 5-strike SMA.
     */
    private List<OiWall> findOiWalls(List<OptionStrike> strikes) {
        List<OiWall> walls = new ArrayList<>();
        
        for (int i = 0; i < strikes.size(); i++) {
            double rollingSma = calculateRollingSma(strikes, i, ROLLING_WINDOW);
            OptionStrike strike = strikes.get(i);
            
            long callOi = strike.getCallOpenInterest() != null ? strike.getCallOpenInterest() : 0;
            long putOi = strike.getPutOpenInterest() != null ? strike.getPutOpenInterest() : 0;
            long totalOi = callOi + putOi;
            
            if (rollingSma > 0 && totalOi >= rollingSma * OI_WALL_MULTIPLIER) {
                OiWall wall = new OiWall();
                wall.setStrike(strike.getStrike());
                wall.setCallOi(callOi);
                wall.setPutOi(putOi);
                wall.setTotalOi(totalOi);
                wall.setSma(rollingSma);
                wall.setRatio(totalOi / rollingSma);
                walls.add(wall);
            }
        }
        
        return walls;
    }
    
    /**
     * Calculates rolling SMA of total OI centered on the given index.
     */
    private double calculateRollingSma(List<OptionStrike> strikes, int centerIndex, int windowSize) {
        int halfWindow = windowSize / 2;
        int start = Math.max(0, centerIndex - halfWindow);
        int end = Math.min(strikes.size(), centerIndex + halfWindow + 1);
        
        double sum = 0;
        int count = 0;
        
        for (int i = start; i < end; i++) {
            if (i != centerIndex) { // Exclude current strike
                OptionStrike s = strikes.get(i);
                long callOi = s.getCallOpenInterest() != null ? s.getCallOpenInterest() : 0;
                long putOi = s.getPutOpenInterest() != null ? s.getPutOpenInterest() : 0;
                sum += callOi + putOi;
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    /**
     * Finds the nearest OI wall to current price.
     */
    private OiWall findNearestWall(List<OiWall> walls, Double spot) {
        if (walls == null || walls.isEmpty() || spot == null) return null;
        
        return walls.stream()
            .min(Comparator.comparing(w -> Math.abs(w.getStrike() - spot)))
            .orElse(null);
    }
    
    /**
     * Calculates Pin Risk Index (PinIndex).
     * Measures the tendency for price to pin to strikes at expiration.
     * 
     * PinIndex considers:
     * - Total OI near current price
     * - Balance of call/put OI
     * - Distance to expiration
     * - Wall proximity
     */
    private double calculatePinRiskIndex(List<OptionStrike> strikes, Double spot) {
        if (spot == null || strikes.isEmpty()) return 0.0;
        
        double totalPinScore = 0.0;
        double totalWeight = 0.0;
        
        for (OptionStrike strike : strikes) {
            double distance = Math.abs(strike.getStrike() - spot) / spot;
            
            // Only consider strikes within 5%
            if (distance > 0.05) continue;
            
            long callOi = strike.getCallOpenInterest() != null ? strike.getCallOpenInterest() : 0;
            long putOi = strike.getPutOpenInterest() != null ? strike.getPutOpenInterest() : 0;
            long totalOi = callOi + putOi;
            
            if (totalOi == 0) continue;
            
            // Weight by proximity and OI
            double proximityWeight = 1.0 - (distance / 0.05);
            double oiWeight = Math.log(1 + totalOi) / 10.0; // Log scale for OI
            double balanceFactor = 1.0 - Math.abs(callOi - putOi) / (double) totalOi; // Balanced OI = higher pin risk
            
            double strikeScore = proximityWeight * oiWeight * balanceFactor;
            totalPinScore += strikeScore;
            totalWeight += proximityWeight;
        }
        
        return totalWeight > 0 ? (totalPinScore / totalWeight) * 100.0 : 0.0;
    }
    
    /**
     * Calculates OI concentration - percentage of total OI in top 3 strikes.
     */
    private double calculateOiConcentration(List<OptionStrike> strikes) {
        if (strikes.isEmpty()) return 0.0;
        
        double totalOi = strikes.stream()
            .mapToDouble(s -> {
                long callOi = s.getCallOpenInterest() != null ? s.getCallOpenInterest() : 0;
                long putOi = s.getPutOpenInterest() != null ? s.getPutOpenInterest() : 0;
                return callOi + putOi;
            })
            .sum();
        
        if (totalOi == 0) return 0.0;
        
        double top3Oi = strikes.stream()
            .sorted(Comparator.comparing(s -> {
                long callOi = s.getCallOpenInterest() != null ? s.getCallOpenInterest() : 0;
                long putOi = s.getPutOpenInterest() != null ? s.getPutOpenInterest() : 0;
                return -(callOi + putOi);
            }))
            .limit(3)
            .mapToDouble(s -> {
                long callOi = s.getCallOpenInterest() != null ? s.getCallOpenInterest() : 0;
                long putOi = s.getPutOpenInterest() != null ? s.getPutOpenInterest() : 0;
                return callOi + putOi;
            })
            .sum();
        
        return (top3Oi / totalOi) * 100.0;
    }
    
    /**
     * Calculates distribution of walls (above/below spot, call/put heavy).
     */
    private WallDistribution calculateWallDistribution(List<OiWall> walls) {
        WallDistribution dist = new WallDistribution();
        if (walls == null || walls.isEmpty()) return dist;
        
        double spot = walls.get(0).getStrike(); // Approximate
        
        for (OiWall wall : walls) {
            if (wall.getStrike() > spot) {
                dist.setWallsAbove(dist.getWallsAbove() + 1);
                dist.setCallOiAbove(dist.getCallOiAbove() + wall.getCallOi());
            } else {
                dist.setWallsBelow(dist.getWallsBelow() + 1);
                dist.setPutOiBelow(dist.getPutOiBelow() + wall.getPutOi());
            }
        }
        
        return dist;
    }
    
    /**
     * Finds max pain strike (minimum dollar value at expiration).
     */
    private double findMaxPain(List<OptionStrike> strikes) {
        if (strikes.isEmpty()) return 0.0;
        
        double minPain = Double.MAX_VALUE;
        double maxPainStrike = strikes.get(0).getStrike();
        
        for (OptionStrike candidate : strikes) {
            double pain = calculatePainAtStrike(strikes, candidate.getStrike());
            if (pain < minPain) {
                minPain = pain;
                maxPainStrike = candidate.getStrike();
            }
        }
        
        return maxPainStrike;
    }
    
    /**
     * Calculates total pain (dollar value) at a given strike.
     */
    private double calculatePainAtStrike(List<OptionStrike> strikes, double priceAtExpiry) {
        double totalPain = 0.0;
        
        for (OptionStrike strike : strikes) {
            long callOi = strike.getCallOpenInterest() != null ? strike.getCallOpenInterest() : 0;
            long putOi = strike.getPutOpenInterest() != null ? strike.getPutOpenInterest() : 0;
            
            // Call pain: max(0, strike - price) * OI
            double callPain = Math.max(0, strike.getStrike() - priceAtExpiry) * callOi;
            // Put pain: max(0, price - strike) * OI
            double putPain = Math.max(0, priceAtExpiry - strike.getStrike()) * putOi;
            
            totalPain += callPain + putPain;
        }
        
        return totalPain;
    }
    
    /**
     * Calculates OI momentum (day-over-day % change).
     * Requires historical data - stub implementation.
     */
    public Double calculateOiMomentum(long currentOi, long previousOi) {
        if (previousOi == 0) return currentOi > 0 ? 100.0 : 0.0;
        return ((currentOi - previousOi) / (double) previousOi) * 100.0;
    }
    
    /**
     * Result container for OI structure calculations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenInterestResult {
        private java.time.LocalDate expiryDate;
        private Integer daysToExpiry;
        private Double underlyingPrice;
        private List<OiWall> oiWalls;
        private int numWalls;
        private OiWall nearestWall;
        private Double nearestWallDistanceDollar;
        private Double nearestWallDistancePercent;
        private double pinRiskIndex;
        private double oiConcentration;
        private WallDistribution wallDistribution;
        private double maxPainStrike;
        private Double maxPainDistance;
        
        public static OpenInterestResult empty() {
            return OpenInterestResult.builder()
                .oiWalls(new ArrayList<>())
                .wallDistribution(new WallDistribution())
                .build();
        }
    }
    
    /**
     * Represents an OI wall.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OiWall {
        private double strike;
        private long callOi;
        private long putOi;
        private long totalOi;
        private double sma;
        private double ratio;
    }
    
    /**
     * Distribution of walls above/below spot.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WallDistribution {
        private int wallsAbove;
        private int wallsBelow;
        private long callOiAbove;
        private long putOiBelow;
    }
}
