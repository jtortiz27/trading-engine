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
 * Volatility Surface Shape calculations per Options Edge spec.
 * - RR25 (Risk Reversal): IV(25d call) - IV(25d put)
 * - BF25 (Butterfly): 0.5*(IV(25d call)+IV(25d put)) - IV(ATM)
 * - Term slope: IV(~30D) - IV(~60D)
 * - Wing-richness z-score
 */
public class VolSurfaceCalculator {

  /**
   * Result container for vol surface calculations.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VolSurfaceResult {
    // Risk Reversal metrics
    private Double rr25; // IV(25d call) - IV(25d put)
    private Double rr10; // IV(10d call) - IV(10d put)

    // Butterfly metrics
    private Double bf25; // 0.5*(IV(25d call)+IV(25d put)) - IV(ATM)
    private Double bf10; // 0.5*(IV(10d call)+IV(10d put)) - IV(ATM)

    // Term structure
    private Double termSlope30_60; // IV(~30D) - IV(~60D)
    private Double termSlope7_30; // IV(~7D) - IV(~30D)

    // ATM IV
    private Double atmIv;
    private Double atmStrike;

    // Wing richness
    private Double wingRichnessZscore;
    private Double wingRichnessPercentile;

    // Skew metrics
    private Double putSkew; // IV(25d put) - IV(ATM)
    private Double callSkew; // IV(25d call) - IV(ATM)
    private Double skewRatio; // putSkew / callSkew

    // IV by delta
    private Map<String, Double> ivByDelta;

    // Surface fit quality
    private Double surfaceFitR2;
  }

  /**
   * Represents a point on the volatility surface.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VolPoint {
    private Double strike;
    private Double delta;
    private Double iv;
    private Double daysToExpiry;
    private Boolean isCall;
  }

  /**
   * Historical volatility data for z-score calculation.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HistoricalVolData {
    private Double avgWingRichness; // Historical average
    private Double stdDevWingRichness; // Historical std dev
    private List<Double> wingRichnessHistory; // Last N observations
  }

  /**
   * Calculate volatility surface metrics.
   *
   * @param volPoints List of IV points (strikes/deltas) for single expiry
   * @param atmStrike The ATM strike price
   * @param spot Current spot price
   * @param historicalVols Optional historical data for z-score
   * @return VolSurfaceResult with all vol metrics
   */
  public static VolSurfaceResult calculateVolSurface(List<VolPoint> volPoints,
                                                      Double atmStrike,
                                                      double spot,
                                                      HistoricalVolData historicalVols) {
    if (volPoints == null || volPoints.isEmpty()) {
      return VolSurfaceResult.builder().build();
    }

    // Find ATM IV
    Double atmIv = findAtmIv(volPoints, atmStrike);

    // Find IV by delta
    Double iv25Call = findIvByDelta(volPoints, 0.25, true);
    Double iv25Put = findIvByDelta(volPoints, -0.25, false);
    Double iv10Call = findIvByDelta(volPoints, 0.10, true);
    Double iv10Put = findIvByDelta(volPoints, -0.10, false);

    // Calculate RR25
    Double rr25 = null;
    if (iv25Call != null && iv25Put != null) {
      rr25 = iv25Call - iv25Put;
    }

    // Calculate RR10
    Double rr10 = null;
    if (iv10Call != null && iv10Put != null) {
      rr10 = iv10Call - iv10Put;
    }

    // Calculate BF25
    Double bf25 = null;
    if (iv25Call != null && iv25Put != null && atmIv != null) {
      bf25 = 0.5 * (iv25Call + iv25Put) - atmIv;
    }

    // Calculate BF10
    Double bf10 = null;
    if (iv10Call != null && iv10Put != null && atmIv != null) {
      bf10 = 0.5 * (iv10Call + iv10Put) - atmIv;
    }

    // Calculate skew
    Double putSkew = null;
    if (iv25Put != null && atmIv != null) {
      putSkew = iv25Put - atmIv;
    }

    Double callSkew = null;
    if (iv25Call != null && atmIv != null) {
      callSkew = iv25Call - atmIv;
    }

    Double skewRatio = null;
    if (putSkew != null && callSkew != null && callSkew != 0) {
      skewRatio = putSkew / callSkew;
    }

    // Calculate wing richness z-score
    Double wingRichnessZscore = null;
    Double wingRichnessPercentile = null;
    if (bf25 != null && historicalVols != null) {
      wingRichnessZscore = calculateWingRichnessZscore(bf25, historicalVols);
      wingRichnessPercentile = calculatePercentile(bf25, historicalVols.getWingRichnessHistory());
    }

    // Build IV by delta map
    Map<String, Double> ivByDelta = new HashMap<>();
    ivByDelta.put("atm", atmIv);
    ivByDelta.put("25Call", iv25Call);
    ivByDelta.put("25Put", iv25Put);
    ivByDelta.put("10Call", iv10Call);
    ivByDelta.put("10Put", iv10Put);

    return VolSurfaceResult.builder()
        .rr25(rr25)
        .rr10(rr10)
        .bf25(bf25)
        .bf10(bf10)
        .atmIv(atmIv)
        .atmStrike(atmStrike)
        .putSkew(putSkew)
        .callSkew(callSkew)
        .skewRatio(skewRatio)
        .wingRichnessZscore(wingRichnessZscore)
        .wingRichnessPercentile(wingRichnessPercentile)
        .ivByDelta(ivByDelta)
        .build();
  }

  /**
   * Calculate term structure slope.
   *
   * @param volSurfaces List of vol surfaces for different expiries
   * @return Term slope metrics
   */
  public static VolSurfaceResult calculateTermStructure(List<VolSurfaceResult> volSurfaces) {
    if (volSurfaces == null || volSurfaces.size() < 2) {
      return VolSurfaceResult.builder().build();
    }

    // Sort by DTE (approximate from context)
    List<VolSurfaceResult> sorted = new ArrayList<>(volSurfaces);

    Double iv30 = null;
    Double iv60 = null;
    Double iv7 = null;

    // Find closest to target DTE (simplified)
    for (VolSurfaceResult vs : sorted) {
      if (iv30 == null && vs.getAtmIv() != null) {
        iv30 = vs.getAtmIv(); // Assume first is ~30D
      } else if (iv60 == null && vs.getAtmIv() != null) {
        iv60 = vs.getAtmIv(); // Assume second is ~60D
      }
    }

    Double termSlope30_60 = null;
    if (iv30 != null && iv60 != null) {
      termSlope30_60 = iv30 - iv60;
    }

    Double termSlope7_30 = null;
    if (iv7 != null && iv30 != null) {
      termSlope7_30 = iv7 - iv30;
    }

    return VolSurfaceResult.builder()
        .termSlope30_60(termSlope30_60)
        .termSlope7_30(termSlope7_30)
        .build();
  }

  /**
   * Find ATM IV from vol points.
   */
  private static Double findAtmIv(List<VolPoint> volPoints, Double atmStrike) {
    if (atmStrike == null) return null;

    // Find closest to ATM strike
    VolPoint closest = null;
    double minDiff = Double.MAX_VALUE;

    for (VolPoint vp : volPoints) {
      if (vp.getStrike() != null && vp.getIv() != null) {
        double diff = Math.abs(vp.getStrike() - atmStrike);
        if (diff < minDiff) {
          minDiff = diff;
          closest = vp;
        }
      }
    }

    return closest != null ? closest.getIv() : null;
  }

  /**
   * Find IV for a specific delta.
   */
  private static Double findIvByDelta(List<VolPoint> volPoints, double targetDelta,
                                      boolean isCall) {
    VolPoint closest = null;
    double minDiff = Double.MAX_VALUE;

    for (VolPoint vp : volPoints) {
      if (vp.getDelta() != null && vp.getIv() != null
          && vp.getIsCall() != null && vp.getIsCall() == isCall) {
        double diff = Math.abs(Math.abs(vp.getDelta()) - Math.abs(targetDelta));
        if (diff < minDiff) {
          minDiff = diff;
          closest = vp;
        }
      }
    }

    return closest != null ? closest.getIv() : null;
  }

  /**
   * Calculate wing richness z-score.
   */
  private static Double calculateWingRichnessZscore(double currentBf25,
                                                     HistoricalVolData historicalVols) {
    if (historicalVols == null || historicalVols.getAvgWingRichness() == null
        || historicalVols.getStdDevWingRichness() == null) {
      return null;
    }

    double avg = historicalVols.getAvgWingRichness();
    double stdDev = historicalVols.getStdDevWingRichness();

    if (stdDev <= 0) return 0.0;

    return (currentBf25 - avg) / stdDev;
  }

  /**
   * Calculate percentile of current value in historical distribution.
   */
  private static Double calculatePercentile(double currentValue, List<Double> history) {
    if (history == null || history.isEmpty()) return null;

    int count = 0;
    for (Double h : history) {
      if (h != null && h <= currentValue) {
        count++;
      }
    }

    return (double) count / history.size();
  }

  /**
   * Interpret volatility skew direction.
   * Positive RR25 = call skew (bullish sentiment)
   * Negative RR25 = put skew (bearish sentiment)
   */
  public static String getSkewSentiment(Double rr25) {
    if (rr25 == null) return "NEUTRAL";
    if (rr25 > 0.02) return "BULLISH";
    if (rr25 < -0.02) return "BEARISH";
    return "NEUTRAL";
  }

  /**
   * Interpret term structure shape.
   * Positive slope = contango (normal)
   * Negative slope = backwardation (elevated near-term fear)
   */
  public static String getTermStructureShape(Double termSlope30_60) {
    if (termSlope30_60 == null) return "FLAT";
    if (termSlope30_60 > 0.01) return "CONTANGO";
    if (termSlope30_60 < -0.01) return "BACKWARDATION";
    return "FLAT";
  }
}
