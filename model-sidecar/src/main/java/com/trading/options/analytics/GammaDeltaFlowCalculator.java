package com.trading.options.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gamma and Delta Flow calculations per Options Edge spec.
 * - Delta-weighted GEX per strike: Gamma * abs(Delta) * S / 100
 * - Zero-gamma flip levels
 * - Net-ATM Gamma Tilt
 */
public class GammaDeltaFlowCalculator {

  /**
   * Result container for GEX calculations.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GexResult {
    // Strike-level data
    private List<StrikeGex> strikeGexData;

    // Zero-gamma flip level (where cumulative GEX crosses zero)
    private Double zeroGammaFlipLevel;

    // Net-ATM Gamma Tilt (sum of gamma above vs below spot)
    private Double netAtmGammaTilt;

    // Total Gamma Exposure across all strikes
    private Double totalGex;

    // Max Gamma Strike (magnet)
    private Double maxGammaStrike;

    // Cumulative GEX per strike (sorted)
    private List<CumulativeGex> cumulativeGex;
  }

  /**
   * GEX data per strike.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StrikeGex {
    private Double strike;
    private Double callGex;
    private Double putGex;
    private Double totalGex;
    private Double deltaWeightedGex; // Gamma * abs(Delta) * Spot / 100
    private Double netDelta; // Call delta + Put delta
  }

  /**
   * Cumulative GEX data point.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CumulativeGex {
    private Double strike;
    private Double cumulativeGex;
  }

  /**
   * Calculate Gamma Exposure for an options chain.
   *
   * @param strikes List of option data at each strike
   * @param spot Current underlying spot price
   * @return GexResult with all GEX calculations
   */
  public static GexResult calculateGex(List<OptionStrikeData> strikes, double spot) {
    if (strikes == null || strikes.isEmpty()) {
      return GexResult.builder().build();
    }

    List<StrikeGex> strikeGexList = new ArrayList<>();
    double totalGex = 0.0;
    double maxGex = 0.0;
    Double maxGammaStrike = null;

    // Calculate per-strike GEX
    for (OptionStrikeData strikeData : strikes) {
      StrikeGex sg = calculateStrikeGex(strikeData, spot);
      strikeGexList.add(sg);
      totalGex += sg.getTotalGex();

      if (sg.getTotalGex() > maxGex) {
        maxGex = sg.getTotalGex();
        maxGammaStrike = sg.getStrike();
      }
    }

    // Sort by strike
    strikeGexList.sort(Comparator.comparing(StrikeGex::getStrike));

    // Calculate cumulative GEX
    List<CumulativeGex> cumulativeGex = new ArrayList<>();
    double runningGex = 0.0;
    for (StrikeGex sg : strikeGexList) {
      runningGex += sg.getTotalGex();
      cumulativeGex.add(CumulativeGex.builder()
          .strike(sg.getStrike())
          .cumulativeGex(runningGex)
          .build());
    }

    // Find zero-gamma flip level (where cumulative GEX crosses zero)
    Double zeroGammaFlip = findZeroGammaFlip(cumulativeGex);

    // Calculate Net-ATM Gamma Tilt
    Double netAtmTilt = calculateNetAtmGammaTilt(strikeGexList, spot);

    return GexResult.builder()
        .strikeGexData(strikeGexList)
        .zeroGammaFlipLevel(zeroGammaFlip)
        .netAtmGammaTilt(netAtmTilt)
        .totalGex(totalGex)
        .maxGammaStrike(maxGammaStrike)
        .cumulativeGex(cumulativeGex)
        .build();
  }

  /**
   * Calculate GEX for a single strike.
   */
  private static StrikeGex calculateStrikeGex(OptionStrikeData strikeData, double spot) {
    double callGex = 0.0;
    double putGex = 0.0;
    double deltaWeightedGex = 0.0;
    double netDelta = 0.0;

    // Call GEX = Gamma * OI * 100 (contract multiplier)
    if (strikeData.getCallGamma() != null && strikeData.getCallOpenInterest() != null) {
      callGex = strikeData.getCallGamma() * strikeData.getCallOpenInterest() * 100.0;
    }

    // Put GEX = Gamma * OI * 100
    if (strikeData.getPutGamma() != null && strikeData.getPutOpenInterest() != null) {
      putGex = strikeData.getPutGamma() * strikeData.getPutOpenInterest() * 100.0;
    }

    // Delta-weighted GEX: Gamma * abs(Delta) * Spot / 100 * OI
    if (strikeData.getCallGamma() != null && strikeData.getCallDelta() != null
        && strikeData.getCallOpenInterest() != null) {
      deltaWeightedGex += strikeData.getCallGamma()
          * Math.abs(strikeData.getCallDelta())
          * spot / 100.0
          * strikeData.getCallOpenInterest();
    }

    if (strikeData.getPutGamma() != null && strikeData.getPutDelta() != null
        && strikeData.getPutOpenInterest() != null) {
      deltaWeightedGex += strikeData.getPutGamma()
          * Math.abs(strikeData.getPutDelta())
          * spot / 100.0
          * strikeData.getPutOpenInterest();
    }

    // Net delta at strike
    if (strikeData.getCallDelta() != null) {
      netDelta += strikeData.getCallDelta();
    }
    if (strikeData.getPutDelta() != null) {
      netDelta += strikeData.getPutDelta();
    }

    return StrikeGex.builder()
        .strike(strikeData.getStrike())
        .callGex(callGex)
        .putGex(putGex)
        .totalGex(callGex + putGex)
        .deltaWeightedGex(deltaWeightedGex)
        .netDelta(netDelta)
        .build();
  }

  /**
   * Find the zero-gamma flip level where cumulative GEX crosses from positive to negative.
   * This is the price level where market makers flip from long gamma to short gamma.
   */
  private static Double findZeroGammaFlip(List<CumulativeGex> cumulativeGex) {
    if (cumulativeGex == null || cumulativeGex.size() < 2) return null;

    for (int i = 1; i < cumulativeGex.size(); i++) {
      double prevGex = cumulativeGex.get(i - 1).getCumulativeGex();
      double currGex = cumulativeGex.get(i).getCumulativeGex();
      double prevStrike = cumulativeGex.get(i - 1).getStrike();
      double currStrike = cumulativeGex.get(i).getStrike();

      // Check for sign change (crossing zero)
      if (prevGex > 0 && currGex <= 0) {
        // Linear interpolation between strikes
        double t = prevGex / (prevGex - currGex);
        return prevStrike + t * (currStrike - prevStrike);
      }
      if (prevGex < 0 && currGex >= 0) {
        double t = Math.abs(prevGex) / (Math.abs(prevGex) + currGex);
        return prevStrike + t * (currStrike - prevStrike);
      }
    }

    return null;
  }

  /**
   * Calculate Net-ATM Gamma Tilt.
   * Positive = more gamma above spot (upside constrained, downside accelerates)
   * Negative = more gamma below spot (downside constrained, upside accelerates)
   */
  private static Double calculateNetAtmGammaTilt(List<StrikeGex> strikeGexList, double spot) {
    if (strikeGexList == null || strikeGexList.isEmpty()) return null;

    double aboveGamma = 0.0;
    double belowGamma = 0.0;

    for (StrikeGex sg : strikeGexList) {
      if (sg.getStrike() > spot) {
        aboveGamma += sg.getTotalGex();
      } else {
        belowGamma += sg.getTotalGex();
      }
    }

    return aboveGamma - belowGamma;
  }

  /**
   * Find the "Gamma Magnet" - the strike with highest absolute GEX.
   */
  public static Optional<Double> findGammaMagnet(GexResult gexResult) {
    if (gexResult == null || gexResult.getStrikeGexData() == null) {
      return Optional.empty();
    }

    return gexResult.getStrikeGexData().stream()
        .max(Comparator.comparing(sg -> Math.abs(sg.getTotalGex())))
        .map(StrikeGex::getStrike);
  }
}
