package com.trading.options.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Realized vs Implied Volatility calculations per Options Edge spec.
 * - HV10, HV20 (close-to-close stdev * sqrt(252))
 * - VRP_ATM = IV_ATM^2 - HV20^2
 * - Gap tape stats (median overnight gap, 90th pct)
 */
public class RealizedVolCalculator {

  private static final double TRADING_DAYS_PER_YEAR = 252.0;

  /**
   * Result container for realized vol calculations.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RealizedVolResult {
    // Historical volatility
    private Double hv10;
    private Double hv20;
    private Double hv30;

    // Realized vol annualized
    private Double realizedVolAnnual;

    // Volatility Risk Premium
    private Double vrpAtm; // IV_ATM^2 - HV20^2
    private Double vrpRatio; // IV_ATM / HV20

    // Gap statistics
    private GapStats gapStats;

    // Vol regime
    private VolRegime volRegime;
  }

  /**
   * Gap statistics.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GapStats {
    private Double medianGapPercent;
    private Double percentile90GapPercent;
    private Double percentile10GapPercent;
    private Double avgAbsoluteGap;
    private Double maxGapUp;
    private Double maxGapDown;
    private Double gapVolatility; // Std dev of gaps
  }

  /**
   * Volatility regime classification.
   */
  public enum VolRegime {
    LOW,       // HV20 < 15%
    NORMAL,    // HV20 15-25%
    ELEVATED,  // HV20 25-35%
    EXTREME    // HV20 > 35%
  }

  /**
   * Calculate historical volatility from price closes.
   *
   * @param closes List of closing prices (oldest to newest)
   * @param atmIv Current ATM implied volatility
   * @param gaps List of overnight gaps (previous close to current open)
   * @return RealizedVolResult with all calculations
   */
  public static RealizedVolResult calculateRealizedVol(List<Double> closes,
                                                        Double atmIv,
                                                        List<Double> gaps) {
    if (closes == null || closes.size() < 2) {
      return RealizedVolResult.builder().build();
    }

    // Calculate log returns
    List<Double> logReturns = new ArrayList<>();
    for (int i = 1; i < closes.size(); i++) {
      double prevClose = closes.get(i - 1);
      double currClose = closes.get(i);
      if (prevClose > 0) {
        double logReturn = Math.log(currClose / prevClose);
        logReturns.add(logReturn);
      }
    }

    // Calculate HV for different windows
    Double hv10 = calculateHv(logReturns, 10);
    Double hv20 = calculateHv(logReturns, 20);
    Double hv30 = calculateHv(logReturns, 30);

    // Calculate VRP
    Double vrpAtm = null;
    Double vrpRatio = null;
    if (atmIv != null && hv20 != null && hv20 > 0) {
      vrpAtm = atmIv * atmIv - hv20 * hv20;
      vrpRatio = atmIv / hv20;
    }

    // Calculate gap stats
    GapStats gapStats = calculateGapStats(gaps);

    // Determine vol regime
    VolRegime regime = determineVolRegime(hv20);

    return RealizedVolResult.builder()
        .hv10(hv10)
        .hv20(hv20)
        .hv30(hv30)
        .realizedVolAnnual(hv20)
        .vrpAtm(vrpAtm)
        .vrpRatio(vrpRatio)
        .gapStats(gapStats)
        .volRegime(regime)
        .build();
  }

  /**
   * Calculate historical volatility for a given window.
   *
   * @param logReturns List of log returns
   * @param window Number of days to use
   * @return Annualized volatility
   */
  private static Double calculateHv(List<Double> logReturns, int window) {
    if (logReturns == null || logReturns.size() < window || window <= 0) {
      return null;
    }

    // Get last 'window' returns
    int startIdx = logReturns.size() - window;
    List<Double> windowReturns = logReturns.subList(startIdx, logReturns.size());

    // Calculate mean
    double sum = 0.0;
    for (double r : windowReturns) {
      sum += r;
    }
    double mean = sum / window;

    // Calculate variance
    double sumSqDiff = 0.0;
    for (double r : windowReturns) {
      double diff = r - mean;
      sumSqDiff += diff * diff;
    }
    double variance = sumSqDiff / (window - 1); // Sample variance

    // Annualize: stdev * sqrt(252)
    return Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
  }

  /**
   * Calculate gap statistics.
   */
  private static GapStats calculateGapStats(List<Double> gaps) {
    if (gaps == null || gaps.isEmpty()) {
      return GapStats.builder().build();
    }

    List<Double> sortedGaps = new ArrayList<>(gaps);
    sortedGaps.sort(Comparator.naturalOrder());

    // Median
    double median = getPercentile(sortedGaps, 0.5);

    // 90th percentile
    double p90 = getPercentile(sortedGaps, 0.9);

    // 10th percentile
    double p10 = getPercentile(sortedGaps, 0.1);

    // Average absolute gap
    double sumAbs = 0.0;
    double maxUp = 0.0;
    double maxDown = 0.0;
    for (double gap : gaps) {
      sumAbs += Math.abs(gap);
      if (gap > maxUp) maxUp = gap;
      if (gap < maxDown) maxDown = gap;
    }
    double avgAbs = sumAbs / gaps.size();

    // Gap volatility (std dev)
    double mean = gaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double sumSq = 0.0;
    for (double gap : gaps) {
      sumSq += (gap - mean) * (gap - mean);
    }
    double gapVol = gaps.size() > 1 ? Math.sqrt(sumSq / (gaps.size() - 1)) : 0.0;

    return GapStats.builder()
        .medianGapPercent(median)
        .percentile90GapPercent(p90)
        .percentile10GapPercent(p10)
        .avgAbsoluteGap(avgAbs)
        .maxGapUp(maxUp)
        .maxGapDown(maxDown)
        .gapVolatility(gapVol)
        .build();
  }

  /**
   * Get percentile value from sorted list.
   */
  private static double getPercentile(List<Double> sorted, double percentile) {
    if (sorted.isEmpty()) return 0.0;
    if (sorted.size() == 1) return sorted.get(0);

    double index = percentile * (sorted.size() - 1);
    int lower = (int) Math.floor(index);
    int upper = (int) Math.ceil(index);

    if (lower == upper) return sorted.get(lower);

    double weight = index - lower;
    return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
  }

  /**
   * Determine volatility regime.
   */
  private static VolRegime determineVolRegime(Double hv20) {
    if (hv20 == null) return VolRegime.NORMAL;

    double hvPct = hv20 * 100; // Convert to percentage

    if (hvPct < 15.0) return VolRegime.LOW;
    if (hvPct > 35.0) return VolRegime.EXTREME;
    if (hvPct > 25.0) return VolRegime.ELEVATED;
    return VolRegime.NORMAL;
  }

  /**
   * Check if VRP is elevated (expensive options).
   */
  public static boolean isVrpElevated(Double vrpRatio) {
    if (vrpRatio == null) return false;
    return vrpRatio > 1.5; // IV is 50% higher than realized
  }

  /**
   * Check if VRP is depressed (cheap options).
   */
  public static boolean isVrpDepressed(Double vrpRatio) {
    if (vrpRatio == null) return false;
    return vrpRatio < 0.8; // IV is 20% lower than realized
  }

  /**
   * Estimate expected move from ATM IV.
   */
  public static Double estimateExpectedMove(double spot, double atmIv, double daysToExpiry) {
    return spot * atmIv * Math.sqrt(daysToExpiry / TRADING_DAYS_PER_YEAR);
  }
}
