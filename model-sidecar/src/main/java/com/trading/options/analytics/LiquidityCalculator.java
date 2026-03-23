package com.trading.options.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Liquidity and Quote Quality calculations per Options Edge spec.
 * - Median and Max bid/ask width % across top-10 OI strikes
 * - "Thin" flag logic
 */
public class LiquidityCalculator {

  // Thresholds for "thin" market classification
  private static final double THIN_MEDIAN_SPREAD_PCT = 0.5; // 0.5% of spot
  private static final double THIN_MAX_SPREAD_PCT = 2.0; // 2% of spot
  private static final long MIN_VOLUME_THRESHOLD = 10; // Min volume for non-thin
  private static final long MIN_OI_THRESHOLD = 100; // Min OI for non-thin

  /**
   * Result container for liquidity calculations.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LiquidityResult {
    // Spread metrics
    private Double medianSpreadPct;
    private Double maxSpreadPct;
    private Double avgSpreadPct;

    // Top 10 OI strikes
    private List<StrikeLiquidity> topOiStrikes;

    // Thin market flag
    private boolean isThin;

    // Detailed flags
    private boolean tooWideMedian;
    private boolean tooWideMax;
    private boolean lowVolume;
    private boolean lowOpenInterest;

    // Liquidity score (0-100, higher is better)
    private Double liquidityScore;

    // Recommendation
    private String liquidityRating; // EXCELLENT, GOOD, FAIR, POOR, THIN
  }

  /**
   * Liquidity data per strike.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StrikeLiquidity {
    private Double strike;
    private Double callSpreadPct;
    private Double putSpreadPct;
    private Double totalOi;
    private Long callVolume;
    private Long putVolume;
    private Double combinedSpreadPct; // Average of call and put spread
  }

  /**
   * Calculate liquidity metrics for an options chain.
   *
   * @param strikes List of option data at each strike
   * @param spot Current underlying spot price
   * @return LiquidityResult with all liquidity calculations
   */
  public static LiquidityResult calculateLiquidity(List<OptionStrikeData> strikes,
                                                    double spot) {
    if (strikes == null || strikes.isEmpty()) {
      return LiquidityResult.builder()
          .isThin(true)
          .liquidityRating("THIN")
          .build();
    }

    // Get top 10 OI strikes
    List<StrikeLiquidity> topStrikes = getTopOiStrikes(strikes, spot, 10);

    // Calculate spread statistics
    List<Double> spreads = new ArrayList<>();
    double totalVolume = 0.0;
    double totalOi = 0.0;

    for (StrikeLiquidity sl : topStrikes) {
      if (sl.getCombinedSpreadPct() != null) {
        spreads.add(sl.getCombinedSpreadPct());
      }
      if (sl.getCallVolume() != null) totalVolume += sl.getCallVolume();
      if (sl.getPutVolume() != null) totalVolume += sl.getPutVolume();
      if (sl.getTotalOi() != null) totalOi += sl.getTotalOi();
    }

    Double medianSpread = calculateMedian(spreads);
    Double maxSpread = spreads.isEmpty() ? null : spreads.stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0.0);
    Double avgSpread = spreads.isEmpty() ? null : spreads.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    // Determine thin flags
    boolean tooWideMedian = medianSpread != null && medianSpread > THIN_MEDIAN_SPREAD_PCT;
    boolean tooWideMax = maxSpread != null && maxSpread > THIN_MAX_SPREAD_PCT;
    boolean lowVolume = totalVolume < MIN_VOLUME_THRESHOLD * 10; // 10x for top 10 strikes
    boolean lowOi = totalOi < MIN_OI_THRESHOLD * 10;

    boolean isThin = tooWideMedian || tooWideMax || (lowVolume && lowOi);

    // Calculate liquidity score
    Double liquidityScore = calculateLiquidityScore(medianSpread, maxSpread, totalVolume, totalOi);

    // Determine rating
    String rating = determineLiquidityRating(liquidityScore, isThin);

    return LiquidityResult.builder()
        .medianSpreadPct(medianSpread)
        .maxSpreadPct(maxSpread)
        .avgSpreadPct(avgSpread)
        .topOiStrikes(topStrikes)
        .isThin(isThin)
        .tooWideMedian(tooWideMedian)
        .tooWideMax(tooWideMax)
        .lowVolume(lowVolume)
        .lowOpenInterest(lowOi)
        .liquidityScore(liquidityScore)
        .liquidityRating(rating)
        .build();
  }

  /**
   * Get top N strikes by open interest.
   */
  private static List<StrikeLiquidity> getTopOiStrikes(List<OptionStrikeData> strikes,
                                                       double spot, int n) {
    List<StrikeLiquidity> allStrikes = new ArrayList<>();

    for (OptionStrikeData data : strikes) {
      Double callSpread = data.getCallSpreadPct(spot);
      Double putSpread = data.getPutSpreadPct(spot);

      Double combinedSpread = null;
      if (callSpread != null && putSpread != null) {
        combinedSpread = (callSpread + putSpread) / 2.0;
      } else if (callSpread != null) {
        combinedSpread = callSpread;
      } else if (putSpread != null) {
        combinedSpread = putSpread;
      }

      allStrikes.add(StrikeLiquidity.builder()
          .strike(data.getStrike())
          .callSpreadPct(callSpread)
          .putSpreadPct(putSpread)
          .totalOi(data.getTotalOpenInterest())
          .callVolume(data.getCallVolume())
          .putVolume(data.getPutVolume())
          .combinedSpreadPct(combinedSpread)
          .build());
    }

    // Sort by OI descending
    allStrikes.sort((a, b) -> {
      double oiA = a.getTotalOi() != null ? a.getTotalOi() : 0.0;
      double oiB = b.getTotalOi() != null ? b.getTotalOi() : 0.0;
      return Double.compare(oiB, oiA);
    });

    // Return top N
    return allStrikes.subList(0, Math.min(n, allStrikes.size()));
  }

  /**
   * Calculate median of a list of values.
   */
  private static Double calculateMedian(List<Double> values) {
    if (values == null || values.isEmpty()) return null;

    List<Double> sorted = new ArrayList<>(values);
    sorted.sort(Comparator.naturalOrder());

    int size = sorted.size();
    if (size % 2 == 0) {
      return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    } else {
      return sorted.get(size / 2);
    }
  }

  /**
   * Calculate liquidity score (0-100).
   */
  private static Double calculateLiquidityScore(Double medianSpread, Double maxSpread,
                                                 double totalVolume, double totalOi) {
    double score = 50.0; // Start at neutral

    // Adjust for spread
    if (medianSpread != null) {
      if (medianSpread < 0.1) score += 25;
      else if (medianSpread < 0.3) score += 15;
      else if (medianSpread < 0.5) score += 5;
      else if (medianSpread > 1.0) score -= 15;
      else if (medianSpread > 0.5) score -= 5;
    }

    if (maxSpread != null) {
      if (maxSpread > 2.0) score -= 20;
      else if (maxSpread > 1.0) score -= 10;
    }

    // Adjust for volume
    if (totalVolume > 10000) score += 15;
    else if (totalVolume > 1000) score += 10;
    else if (totalVolume > 100) score += 5;
    else if (totalVolume < 50) score -= 10;

    // Adjust for OI
    if (totalOi > 100000) score += 10;
    else if (totalOi > 10000) score += 5;
    else if (totalOi < 1000) score -= 10;

    return Math.max(0.0, Math.min(100.0, score));
  }

  /**
   * Determine liquidity rating based on score.
   */
  private static String determineLiquidityRating(Double score, boolean isThin) {
    if (isThin) return "THIN";
    if (score == null) return "UNKNOWN";
    if (score >= 80) return "EXCELLENT";
    if (score >= 60) return "GOOD";
    if (score >= 40) return "FAIR";
    return "POOR";
  }

  /**
   * Check if a specific strike has acceptable liquidity for trading.
   */
  public static boolean isStrikeLiquid(OptionStrikeData strikeData, double spot,
                                       double maxSpreadThreshold) {
    Double callSpread = strikeData.getCallSpreadPct(spot);
    Double putSpread = strikeData.getPutSpreadPct(spot);

    double avgSpread = 0.0;
    int count = 0;
    if (callSpread != null) {
      avgSpread += callSpread;
      count++;
    }
    if (putSpread != null) {
      avgSpread += putSpread;
      count++;
    }

    if (count == 0) return false;
    avgSpread /= count;

    return avgSpread <= maxSpreadThreshold;
  }
}
