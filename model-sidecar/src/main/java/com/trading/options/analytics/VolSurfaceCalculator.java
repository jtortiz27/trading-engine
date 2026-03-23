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
 * - Risk Reversal (RR25): Implied Volatility at 25-delta call minus IV at 25-delta put
 * - Butterfly (BF25): Average IV of 25-delta options minus ATM IV
 * - Term slope: IV difference between short and long dated options
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
    // Risk Reversal metrics - measures directional sentiment via skew difference
    private Double riskReversal25Delta; // IV(25d call) - IV(25d put)
    private Double riskReversal10Delta; // IV(10d call) - IV(10d put)

    // Butterfly metrics - measures wing richness (kurtosis of vol surface)
    private Double butterfly25Delta; // 0.5*(IV(25d call)+IV(25d put)) - IV(ATM)
    private Double butterfly10Delta; // 0.5*(IV(10d call)+IV(10d put)) - IV(ATM)

    // Term structure - how IV varies across expiration dates
    private Double termStructureSlope30to60Days; // IV(~30D) - IV(~60D)
    private Double termStructureSlope7to30Days; // IV(~7D) - IV(~30D)

    // At-the-money IV
    private Double atTheMoneyImpliedVolatility;
    private Double atTheMoneyStrikePrice;

    // Wing richness - how expensive wings are vs historical
    private Double wingRichnessZscore;
    private Double wingRichnessPercentile;

    // Skew metrics - asymmetry between put and call implied volatility
    private Double putSkew; // IV(25d put) - IV(ATM)
    private Double callSkew; // IV(25d call) - IV(ATM)
    private Double putToCallSkewRatio; // putSkew / callSkew

    // IV by delta
    private Map<String, Double> impliedVolatilityByDelta;

    // Surface fit quality
    private Double surfaceFitRSquared;
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
    private Double impliedVolatility;
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
    private Double averageWingRichness; // Historical average
    private Double standardDeviationWingRichness; // Historical std dev
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
    Double atTheMoneyImpliedVolatility = findAtTheMoneyImpliedVolatility(volPoints, atmStrike);

    // Find IV by delta
    Double impliedVolatility25DeltaCall = findImpliedVolatilityByDelta(volPoints, 0.25, true);
    Double impliedVolatility25DeltaPut = findImpliedVolatilityByDelta(volPoints, -0.25, false);
    Double impliedVolatility10DeltaCall = findImpliedVolatilityByDelta(volPoints, 0.10, true);
    Double impliedVolatility10DeltaPut = findImpliedVolatilityByDelta(volPoints, -0.10, false);

    // Calculate Risk Reversal at 25 delta
    Double riskReversal25Delta = null;
    if (impliedVolatility25DeltaCall != null && impliedVolatility25DeltaPut != null) {
      riskReversal25Delta = impliedVolatility25DeltaCall - impliedVolatility25DeltaPut;
    }

    // Calculate Risk Reversal at 10 delta
    Double riskReversal10Delta = null;
    if (impliedVolatility10DeltaCall != null && impliedVolatility10DeltaPut != null) {
      riskReversal10Delta = impliedVolatility10DeltaCall - impliedVolatility10DeltaPut;
    }

    // Calculate Butterfly at 25 delta
    Double butterfly25Delta = null;
    if (impliedVolatility25DeltaCall != null && impliedVolatility25DeltaPut != null
        && atTheMoneyImpliedVolatility != null) {
      butterfly25Delta = 0.5 * (impliedVolatility25DeltaCall + impliedVolatility25DeltaPut)
          - atTheMoneyImpliedVolatility;
    }

    // Calculate Butterfly at 10 delta
    Double butterfly10Delta = null;
    if (impliedVolatility10DeltaCall != null && impliedVolatility10DeltaPut != null
        && atTheMoneyImpliedVolatility != null) {
      butterfly10Delta = 0.5 * (impliedVolatility10DeltaCall + impliedVolatility10DeltaPut)
          - atTheMoneyImpliedVolatility;
    }

    // Calculate skew
    Double putSkew = null;
    if (impliedVolatility25DeltaPut != null && atTheMoneyImpliedVolatility != null) {
      putSkew = impliedVolatility25DeltaPut - atTheMoneyImpliedVolatility;
    }

    Double callSkew = null;
    if (impliedVolatility25DeltaCall != null && atTheMoneyImpliedVolatility != null) {
      callSkew = impliedVolatility25DeltaCall - atTheMoneyImpliedVolatility;
    }

    Double putToCallSkewRatio = null;
    if (putSkew != null && callSkew != null && callSkew != 0) {
      putToCallSkewRatio = putSkew / callSkew;
    }

    // Calculate wing richness z-score
    Double wingRichnessZscore = null;
    Double wingRichnessPercentile = null;
    if (butterfly25Delta != null && historicalVols != null) {
      wingRichnessZscore = calculateWingRichnessZscore(butterfly25Delta, historicalVols);
      wingRichnessPercentile = calculatePercentile(
          butterfly25Delta, historicalVols.getWingRichnessHistory());
    }

    // Build IV by delta map
    Map<String, Double> impliedVolatilityByDelta = new HashMap<>();
    impliedVolatilityByDelta.put("atm", atTheMoneyImpliedVolatility);
    impliedVolatilityByDelta.put("25Call", impliedVolatility25DeltaCall);
    impliedVolatilityByDelta.put("25Put", impliedVolatility25DeltaPut);
    impliedVolatilityByDelta.put("10Call", impliedVolatility10DeltaCall);
    impliedVolatilityByDelta.put("10Put", impliedVolatility10DeltaPut);

    return VolSurfaceResult.builder()
        .riskReversal25Delta(riskReversal25Delta)
        .riskReversal10Delta(riskReversal10Delta)
        .butterfly25Delta(butterfly25Delta)
        .butterfly10Delta(butterfly10Delta)
        .atTheMoneyImpliedVolatility(atTheMoneyImpliedVolatility)
        .atTheMoneyStrikePrice(atmStrike)
        .putSkew(putSkew)
        .callSkew(callSkew)
        .putToCallSkewRatio(putToCallSkewRatio)
        .wingRichnessZscore(wingRichnessZscore)
        .wingRichnessPercentile(wingRichnessPercentile)
        .impliedVolatilityByDelta(impliedVolatilityByDelta)
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

    Double impliedVolatility30Days = null;
    Double impliedVolatility60Days = null;
    Double impliedVolatility7Days = null;

    // Find closest to target DTE (simplified)
    for (VolSurfaceResult volSurface : sorted) {
      if (impliedVolatility30Days == null && volSurface.getAtTheMoneyImpliedVolatility() != null) {
        impliedVolatility30Days = volSurface.getAtTheMoneyImpliedVolatility(); // Assume first is ~30D
      } else if (impliedVolatility60Days == null
          && volSurface.getAtTheMoneyImpliedVolatility() != null) {
        impliedVolatility60Days = volSurface.getAtTheMoneyImpliedVolatility(); // Assume second is ~60D
      }
    }

    Double termStructureSlope30to60Days = null;
    if (impliedVolatility30Days != null && impliedVolatility60Days != null) {
      termStructureSlope30to60Days = impliedVolatility30Days - impliedVolatility60Days;
    }

    Double termStructureSlope7to30Days = null;
    if (impliedVolatility7Days != null && impliedVolatility30Days != null) {
      termStructureSlope7to30Days = impliedVolatility7Days - impliedVolatility30Days;
    }

    return VolSurfaceResult.builder()
        .termStructureSlope30to60Days(termStructureSlope30to60Days)
        .termStructureSlope7to30Days(termStructureSlope7to30Days)
        .build();
  }

  /**
   * Find ATM IV from vol points.
   */
  private static Double findAtTheMoneyImpliedVolatility(
      List<VolPoint> volPoints, Double atTheMoneyStrike) {
    if (atTheMoneyStrike == null) {
      return null;
    }

    // Find closest to ATM strike
    VolPoint closest = null;
    double minDiff = Double.MAX_VALUE;

    for (VolPoint point : volPoints) {
      if (point.getStrike() != null && point.getImpliedVolatility() != null) {
        double diff = Math.abs(point.getStrike() - atTheMoneyStrike);
        if (diff < minDiff) {
          minDiff = diff;
          closest = point;
        }
      }
    }

    return closest != null ? closest.getImpliedVolatility() : null;
  }

  /**
   * Find IV for a specific delta.
   */
  private static Double findImpliedVolatilityByDelta(
      List<VolPoint> volPoints, double targetDelta, boolean isCall) {
    VolPoint closest = null;
    double minDiff = Double.MAX_VALUE;

    for (VolPoint point : volPoints) {
      if (point.getDelta() != null && point.getImpliedVolatility() != null
          && point.getIsCall() != null && point.getIsCall() == isCall) {
        double diff = Math.abs(Math.abs(point.getDelta()) - Math.abs(targetDelta));
        if (diff < minDiff) {
          minDiff = diff;
          closest = point;
        }
      }
    }

    return closest != null ? closest.getImpliedVolatility() : null;
  }

  /**
   * Calculate wing richness z-score.
   */
  private static Double calculateWingRichnessZscore(
      double currentButterfly25Delta, HistoricalVolData historicalVols) {
    if (historicalVols == null || historicalVols.getAverageWingRichness() == null
        || historicalVols.getStandardDeviationWingRichness() == null) {
      return null;
    }

    double average = historicalVols.getAverageWingRichness();
    double standardDeviation = historicalVols.getStandardDeviationWingRichness();

    if (standardDeviation <= 0) {
      return 0.0;
    }

    return (currentButterfly25Delta - average) / standardDeviation;
  }

  /**
   * Calculate percentile of current value in historical distribution.
   */
  private static Double calculatePercentile(double currentValue, List<Double> history) {
    if (history == null || history.isEmpty()) {
      return null;
    }

    int count = 0;
    for (Double historicalValue : history) {
      if (historicalValue != null && historicalValue <= currentValue) {
        count++;
      }
    }

    return (double) count / history.size();
  }

  /**
   * Interpret volatility skew direction.
   * Positive riskReversal25Delta = call skew (bullish sentiment)
   * Negative riskReversal25Delta = put skew (bearish sentiment)
   */
  public static String getSkewSentiment(Double riskReversal25Delta) {
    if (riskReversal25Delta == null) {
      return "NEUTRAL";
    }
    if (riskReversal25Delta > 0.02) {
      return "BULLISH";
    }
    if (riskReversal25Delta < -0.02) {
      return "BEARISH";
    }
    return "NEUTRAL";
  }

  /**
   * Interpret term structure shape.
   * Positive slope = contango (normal)
   * Negative slope = backwardation (elevated near-term fear)
   */
  public static String getTermStructureShape(Double termStructureSlope30to60Days) {
    if (termStructureSlope30to60Days == null) {
      return "FLAT";
    }
    if (termStructureSlope30to60Days > 0.01) {
      return "CONTANGO";
    }
    if (termStructureSlope30to60Days < -0.01) {
      return "BACKWARDATION";
    }
    return "FLAT";
  }
}
