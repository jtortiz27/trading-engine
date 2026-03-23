package com.trading.options.analytics;

import com.trading.options.analytics.GammaDeltaFlowCalculator.GexResult;
import com.trading.options.analytics.IronFlyCalculator.FlyContext;
import com.trading.options.analytics.IronFlyCalculator.FlyResult;
import com.trading.options.analytics.LiquidityCalculator.LiquidityResult;
import com.trading.options.analytics.OpenInterestCalculator.OiResult;
import com.trading.options.analytics.RealizedVolCalculator.RealizedVolResult;
import com.trading.options.analytics.VolSurfaceCalculator.VolSurfaceResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Main service for options analytics calculations.
 * Aggregates all calculators and provides trade idea ranking.
 */
@Service
public class OptionsAnalyticsService {

  /**
   * Complete options analytics result.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AnalyticsResult {
    private String underlying;
    private Double spot;
    private String expiry;

    // Component results
    private GexResult gexResult;
    private OiResult oiResult;
    private VolSurfaceResult volSurface;
    private RealizedVolResult realizedVol;
    private LiquidityResult liquidity;
    private FlyResult flyResult;

    // Composite signals
    private String overallSignal; // BULLISH, BEARISH, NEUTRAL
    private Double confidenceScore; // 0-100
    private List<TradeIdea> rankedTradeIdeas;
  }

  /**
   * Trade idea with ranking.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TradeIdea {
    private String strategy; // IRON_FLY, IRON_CONDOR, etc.
    private Integer rank;
    private Double conviction;
    private String urgency; // HIGH, MEDIUM, LOW
    private String rationale;
    private List<String> keyMetrics;
  }

  /**
   * Analyze complete options chain.
   */
  public AnalyticsResult analyzeOptionsChain(String underlying, double spot,
                                               List<OptionStrikeData> strikes,
                                               String expiry,
                                               List<Double> priceHistory,
                                               List<Double> gaps) {

    // 1. Calculate GEX
    GexResult gexResult = GammaDeltaFlowCalculator.calculateGex(strikes, spot);

    // 2. Calculate OI Structure
    OiResult oiResult = OpenInterestCalculator.calculateOiStructure(strikes, spot, null);

    // 3. Calculate Vol Surface
    List<VolSurfaceCalculator.VolPoint> volPoints = extractVolPoints(strikes);
    VolSurfaceResult volSurface = VolSurfaceCalculator.calculateVolSurface(
        volPoints, findAtmStrike(strikes, spot), spot, null);

    // 4. Calculate Realized Vol
    RealizedVolResult realizedVol = RealizedVolCalculator.calculateRealizedVol(
        priceHistory, volSurface.getAtmIv(), gaps);

    // 5. Calculate Liquidity
    LiquidityResult liquidity = LiquidityCalculator.calculateLiquidity(strikes, spot);

    // 6. Construct Fly and calculate edge metrics
    FlyContext flyContext = FlyContext.builder()
        .spot(spot)
        .forwardPrice(spot) // Simplified
        .daysToExpiry(30) // Placeholder
        .atmIv(volSurface.getAtmIv() != null ? volSurface.getAtmIv() : 0.2)
        .gexResult(gexResult)
        .oiResult(oiResult)
        .volSurface(volSurface)
        .realizedVol(realizedVol)
        .liquidity(liquidity)
        .build();

    FlyResult flyResult = IronFlyCalculator.calculateIronFly(strikes, flyContext);

    // 7. Determine overall signal
    String signal = determineOverallSignal(gexResult, oiResult, volSurface);
    Double confidence = calculateConfidence(gexResult, oiResult, volSurface, liquidity);

    // 8. Rank trade ideas
    List<TradeIdea> tradeIdeas = rankTradeIdeas(flyResult, gexResult, oiResult, volSurface);

    return AnalyticsResult.builder()
        .underlying(underlying)
        .spot(spot)
        .expiry(expiry)
        .gexResult(gexResult)
        .oiResult(oiResult)
        .volSurface(volSurface)
        .realizedVol(realizedVol)
        .liquidity(liquidity)
        .flyResult(flyResult)
        .overallSignal(signal)
        .confidenceScore(confidence)
        .rankedTradeIdeas(tradeIdeas)
        .build();
  }

  /**
   * Determine overall directional signal.
   */
  private String determineOverallSignal(GexResult gex, OiResult oi, VolSurfaceResult vol) {
    int bullishPoints = 0;
    int bearishPoints = 0;

    // GEX tilt
    if (gex != null && gex.getNetAtmGammaTilt() != null) {
      if (gex.getNetAtmGammaTilt() < 0) bullishPoints++;
      if (gex.getNetAtmGammaTilt() > 0) bearishPoints++;
    }

    // RR25
    if (vol != null && vol.getRr25() != null) {
      if (vol.getRr25() > 0.02) bullishPoints++;
      if (vol.getRr25() < -0.02) bearishPoints++;
    }

    // Put/call ratio
    if (oi != null && oi.getPutCallRatio() != null) {
      if (oi.getPutCallRatio() > 1.2) bullishPoints++; // Extreme fear
      if (oi.getPutCallRatio() < 0.8) bearishPoints++;
    }

    if (bullishPoints > bearishPoints) return "BULLISH";
    if (bearishPoints > bullishPoints) return "BEARISH";
    return "NEUTRAL";
  }

  /**
   * Calculate confidence score.
   */
  private Double calculateConfidence(GexResult gex, OiResult oi,
                                     VolSurfaceResult vol, LiquidityResult liq) {
    double score = 50.0;

    // Adjust for liquidity
    if (liq != null && liq.getLiquidityScore() != null) {
      score += (liq.getLiquidityScore() - 50) * 0.2;
    }

    // Adjust for data quality
    if (gex != null && gex.getTotalGex() != null && gex.getTotalGex() > 0) score += 10;
    if (oi != null && oi.getMaxOiAmount() != null && oi.getMaxOiAmount() > 10000) score += 10;

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Rank trade ideas by conviction.
   */
  private List<TradeIdea> rankTradeIdeas(FlyResult fly, GexResult gex,
                                           OiResult oi, VolSurfaceResult vol) {
    List<TradeIdea> ideas = new ArrayList<>();

    if (fly != null && fly.getFlyConviction() != null && fly.getFlyConviction() > 20) {
      String strategy = fly.isUsedIronCondor() ? "IRON_CONDOR" : "IRON_FLY";
      ideas.add(TradeIdea.builder()
          .strategy(strategy)
          .conviction(fly.getFlyConviction())
          .urgency(fly.getUrgencyRating())
          .rationale("Pin risk elevated, gamma magnet near strikes")
          .build());
    }

    // Sort by conviction descending
    ideas.sort(Comparator.comparing(TradeIdea::getConviction).reversed());

    // Assign ranks
    for (int i = 0; i < ideas.size(); i++) {
      ideas.get(i).setRank(i + 1);
    }

    return ideas;
  }

  /**
   * Extract volatility points from strikes.
   */
  private List<VolSurfaceCalculator.VolPoint> extractVolPoints(List<OptionStrikeData> strikes) {
    List<VolSurfaceCalculator.VolPoint> points = new ArrayList<>();

    for (OptionStrikeData strike : strikes) {
      // Call point
      if (strike.getCallIv() != null) {
        points.add(VolSurfaceCalculator.VolPoint.builder()
            .strike(strike.getStrike())
            .delta(strike.getCallDelta())
            .iv(strike.getCallIv())
            .isCall(true)
            .build());
      }

      // Put point
      if (strike.getPutIv() != null) {
        points.add(VolSurfaceCalculator.VolPoint.builder()
            .strike(strike.getStrike())
            .delta(strike.getPutDelta())
            .iv(strike.getPutIv())
            .isCall(false)
            .build());
      }
    }

    return points;
  }

  /**
   * Find ATM strike.
   */
  private Double findAtmStrike(List<OptionStrikeData> strikes, double spot) {
    return strikes.stream()
        .min(Comparator.comparing(s -> Math.abs(s.getStrike() - spot)))
        .map(OptionStrikeData::getStrike)
        .orElse(null);
  }
}
