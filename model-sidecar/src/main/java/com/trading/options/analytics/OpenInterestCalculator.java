package com.trading.options.analytics;

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
 * Open Interest Structure calculations per Options Edge spec.
 * - OI Walls (OI >= 5x rolling 5-strike SMA)
 * - OI Momentum (day-over-day %)
 * - Pin-Risk Index (PinIndex)
 * - Wall distance ($ and %)
 */
public class OpenInterestCalculator {

  private static final double OI_WALL_MULTIPLIER = 5.0;
  private static final int ROLLING_WINDOW = 5;

  /**
   * Result container for OI calculations.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OiResult {
    // OI Walls (high concentration strikes)
    private List<OiWall> oiWalls;

    // Strike with highest total OI
    private Double maxOiStrike;
    private Double maxOiAmount;

    // Call wall (highest call OI)
    private Double callWallStrike;
    private Double callWallOi;

    // Put wall (highest put OI)
    private Double putWallStrike;
    private Double putWallOi;

    // Pin-Risk Index (0-1, higher = more pin risk)
    private Double pinRiskIndex;

    // Distance metrics
    private Double nearestWallDistanceDollars;
    private Double nearestWallDistancePercent;

    // Put/Call ratio
    private Double putCallRatio;
  }

  /**
   * Represents an OI Wall (high concentration strike).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OiWall {
    private Double strike;
    private Double totalOi;
    private Double callOi;
    private Double putOi;
    private Double rollingSma; // 5-strike rolling SMA
    private Double ratioToSma; // OI / SMA
    private Double distanceFromSpotDollars;
    private Double distanceFromSpotPercent;
  }

  /**
   * Historical OI data for momentum calculation.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HistoricalOi {
    private Double strike;
    private Double previousCallOi;
    private Double previousPutOi;
    private Double previousTotalOi;
  }

  /**
   * Calculate Open Interest structure for an options chain.
   *
   * @param strikes List of option data at each strike
   * @param spot Current underlying spot price
   * @param historicalOi Optional historical OI data for momentum calc
   * @return OiResult with all OI calculations
   */
  public static OiResult calculateOiStructure(List<OptionStrikeData> strikes,
                                               double spot,
                                               List<HistoricalOi> historicalOi) {
    if (strikes == null || strikes.isEmpty()) {
      return OiResult.builder().build();
    }

    // Sort by strike
    List<OptionStrikeData> sortedStrikes = new ArrayList<>(strikes);
    sortedStrikes.sort(Comparator.comparing(OptionStrikeData::getStrike));

    // Calculate OI walls
    List<OiWall> oiWalls = identifyOiWalls(sortedStrikes, spot);

    // Find max OI strike
    Double maxOiStrike = null;
    Double maxOiAmount = 0.0;
    Double callWallStrike = null;
    Double callWallOi = 0.0;
    Double putWallStrike = null;
    Double putWallOi = 0.0;

    double totalCallOi = 0.0;
    double totalPutOi = 0.0;

    for (OptionStrikeData strikeData : sortedStrikes) {
      double totalOi = strikeData.getTotalOpenInterest();
      double callOi = strikeData.getCallOpenInterest() != null
          ? strikeData.getCallOpenInterest().doubleValue() : 0.0;
      double putOi = strikeData.getPutOpenInterest() != null
          ? strikeData.getPutOpenInterest().doubleValue() : 0.0;

      totalCallOi += callOi;
      totalPutOi += putOi;

      if (totalOi > maxOiAmount) {
        maxOiAmount = totalOi;
        maxOiStrike = strikeData.getStrike();
      }

      if (callOi > callWallOi) {
        callWallOi = callOi;
        callWallStrike = strikeData.getStrike();
      }

      if (putOi > putWallOi) {
        putWallOi = putOi;
        putWallStrike = strikeData.getStrike();
      }
    }

    // Calculate Pin-Risk Index
    Double pinRiskIndex = calculatePinRiskIndex(sortedStrikes, spot);

    // Calculate nearest wall distances
    Double nearestWallDistDollars = calculateNearestWallDistance(oiWalls, spot);
    Double nearestWallDistPct = nearestWallDistDollars != null
        ? nearestWallDistDollars / spot * 100.0 : null;

    // Put/Call ratio
    Double putCallRatio = totalCallOi > 0 ? totalPutOi / totalCallOi : null;

    return OiResult.builder()
        .oiWalls(oiWalls)
        .maxOiStrike(maxOiStrike)
        .maxOiAmount(maxOiAmount)
        .callWallStrike(callWallStrike)
        .callWallOi(callWallOi)
        .putWallStrike(putWallStrike)
        .putWallOi(putWallOi)
        .pinRiskIndex(pinRiskIndex)
        .nearestWallDistanceDollars(nearestWallDistDollars)
        .nearestWallDistancePercent(nearestWallDistPct)
        .putCallRatio(putCallRatio)
        .build();
  }

  /**
   * Identify OI Walls (strikes with OI >= 5x rolling 5-strike SMA).
   */
  private static List<OiWall> identifyOiWalls(List<OptionStrikeData> sortedStrikes, double spot) {
    List<OiWall> walls = new ArrayList<>();
    int halfWindow = ROLLING_WINDOW / 2;

    for (int i = 0; i < sortedStrikes.size(); i++) {
      OptionStrikeData current = sortedStrikes.get(i);
      double totalOi = current.getTotalOpenInterest();

      // Calculate rolling SMA (5-strike window centered on current)
      int startIdx = Math.max(0, i - halfWindow);
      int endIdx = Math.min(sortedStrikes.size(), i + halfWindow + 1);
      int count = 0;
      double sum = 0.0;

      for (int j = startIdx; j < endIdx; j++) {
        if (j != i) { // Exclude current strike
          sum += sortedStrikes.get(j).getTotalOpenInterest();
          count++;
        }
      }

      if (count > 0) {
        double sma = sum / count;
        double ratio = totalOi / sma;

        if (ratio >= OI_WALL_MULTIPLIER) {
          double callOi = current.getCallOpenInterest() != null
              ? current.getCallOpenInterest().doubleValue() : 0.0;
          double putOi = current.getPutOpenInterest() != null
              ? current.getPutOpenInterest().doubleValue() : 0.0;

          walls.add(OiWall.builder()
              .strike(current.getStrike())
              .totalOi(totalOi)
              .callOi(callOi)
              .putOi(putOi)
              .rollingSma(sma)
              .ratioToSma(ratio)
              .distanceFromSpotDollars(Math.abs(current.getStrike() - spot))
              .distanceFromSpotPercent(Math.abs(current.getStrike() - spot) / spot * 100.0)
              .build());
        }
      }
    }

    return walls;
  }

  /**
   * Calculate Pin-Risk Index (0-1 scale).
   * Higher values indicate higher probability of pinning at expiry.
   * Based on OI concentration near spot relative to total OI.
   */
  private static Double calculatePinRiskIndex(List<OptionStrikeData> sortedStrikes, double spot) {
    if (sortedStrikes == null || sortedStrikes.isEmpty()) return null;

    // Define ATM zone as +/- 1% from spot
    double atmLower = spot * 0.99;
    double atmUpper = spot * 1.01;

    double atmOi = 0.0;
    double totalOi = 0.0;

    for (OptionStrikeData strikeData : sortedStrikes) {
      double strikeOi = strikeData.getTotalOpenInterest();
      totalOi += strikeOi;

      if (strikeData.getStrike() >= atmLower && strikeData.getStrike() <= atmUpper) {
        atmOi += strikeOi;
      }
    }

    if (totalOi <= 0) return 0.0;

    // Pin risk is proportional to OI concentration in ATM zone
    // Normalize to 0-1 scale
    double concentration = atmOi / totalOi;
    return Math.min(1.0, concentration * 5.0); // Scale factor to normalize
  }

  /**
   * Calculate distance to nearest OI wall.
   */
  private static Double calculateNearestWallDistance(List<OiWall> walls, double spot) {
    if (walls == null || walls.isEmpty()) return null;

    return walls.stream()
        .mapToDouble(OiWall::getDistanceFromSpotDollars)
        .min()
        .orElse(0.0);
  }

  /**
   * Calculate OI Momentum (day-over-day % change).
   *
   * @param currentOi Current OI data
   * @param historicalOi Previous day's OI data
   * @return Map of strike -> momentum %
   */
  public static Map<Double, Double> calculateOiMomentum(List<OptionStrikeData> currentOi,
                                                      List<HistoricalOi> historicalOi) {
    Map<Double, Double> momentumMap = new HashMap<>();

    if (historicalOi == null || historicalOi.isEmpty()) return momentumMap;

    // Build lookup map for historical OI
    Map<Double, Double> historicalOiMap = new HashMap<>();
    for (HistoricalOi hist : historicalOi) {
      if (hist.getPreviousTotalOi() != null) {
        historicalOiMap.put(hist.getStrike(), hist.getPreviousTotalOi());
      }
    }

    // Calculate momentum for each strike
    for (OptionStrikeData current : currentOi) {
      double currentTotalOi = current.getTotalOpenInterest();
      Double prevOi = historicalOiMap.get(current.getStrike());

      if (prevOi != null && prevOi > 0) {
        double momentum = (currentTotalOi - prevOi) / prevOi * 100.0;
        momentumMap.put(current.getStrike(), momentum);
      }
    }

    return momentumMap;
  }

  /**
   * Check if there's a double pin (high call OI above and high put OI below spot).
   * This creates a "pin zone" where price is likely to stay.
   */
  public static boolean hasDoublePin(OiResult oiResult, double spot, double tolerance) {
    if (oiResult == null || oiResult.getOiWalls() == null) return false;

    boolean hasCallWallAbove = false;
    boolean hasPutWallBelow = false;

    for (OiWall wall : oiResult.getOiWalls()) {
      if (wall.getStrike() > spot * (1 + tolerance)) {
        if (wall.getCallOi() > wall.getPutOi() * 1.5) {
          hasCallWallAbove = true;
        }
      }
      if (wall.getStrike() < spot * (1 - tolerance)) {
        if (wall.getPutOi() > wall.getCallOi() * 1.5) {
          hasPutWallBelow = true;
        }
      }
    }

    return hasCallWallAbove && hasPutWallBelow;
  }
}
