package com.trading.options.analytics;

import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Iron Fly Edge Metrics calculations per Options Edge spec.
 *
 * An Iron Fly is constructed by:
 * - Selling ATM call and put (the "body")
 * - Buying OTM call and put (the "wings") equidistant from body
 *
 * Key Metrics:
 * - FlyCenterScore (PinIndex + proximity + Gamma Tilt + OI surge)
 * - WingRichnessScore
 * - Theta-to-Move (T/M)
 * - BEPG (breach probability)
 * - CpR (Credit-per-Risk)
 * - MRB (Magnet-Risk Balance)
 */
public class IronFlyCalculator {

  // Risk thresholds
  private static final double MIN_FLY_WIDTH_PCT = 0.01; // 1%
  private static final double MAX_FLY_WIDTH_PCT = 0.05; // 5%
  private static final int MAX_WING_WIDEN_ITERATIONS = 3;

  /**
   * Result container for Iron Fly metrics.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FlyResult {
    // Fly structure
    private FlyStructure structure;

    // Edge metrics
    private Double flyCenterScore;
    private Double wingRichnessScore;
    private Double thetaToMove; // T/M
    private Double bepg; // Breach probability
    private Double cpr; // Credit-per-Risk
    private Double mrb; // Magnet-Risk Balance

    // Conviction score
    private Double flyConviction;
    private String urgencyRating; // HIGH, MEDIUM, LOW

    // Fallback
    private boolean usedIronCondor;
    private FlyStructure condorStructure;
  }

  /**
   * Structure of an Iron Fly trade.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FlyStructure {
    private Double shortCallStrike; // Body - short ATM call
    private Double shortPutStrike;  // Body - short ATM put
    private Double longCallStrike;  // Wing - long OTM call
    private Double longPutStrike;   // Wing - long OTM put

    private Double centerStrike;    // Same as short strikes (for fly)
    private Double wingWidth;       // Distance from center to wing

    // Premiums
    private Double callCredit;      // Short call premium - long call premium
    private Double putCredit;       // Short put premium - long put premium
    private Double totalCredit;     // Combined credit received

    // Max risk (distance between body and wing minus credit)
    private Double maxRisk;

    // Greeks
    private Double netDelta;
    private Double netGamma;
    private Double netTheta;
    private Double netVega;

    // Breakevens
    private Double upperBreakeven;
    private Double lowerBreakeven;

    // Profit zone
    private Double profitZoneWidth;
    private Double profitZonePercent;
  }

  /**
   * Input context for fly calculation.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FlyContext {
    private double spot;
    private double forwardPrice;
    private double daysToExpiry;
    private double atTheMoneyImpliedVolatility;

    // Analytics results
    private GammaDeltaFlowCalculator.GexResult gexResult;
    private OpenInterestCalculator.OiResult oiResult;
    private VolSurfaceCalculator.VolSurfaceResult volSurface;
    private RealizedVolCalculator.RealizedVolResult realizedVol;
    private LiquidityCalculator.LiquidityResult liquidity;
  }

  /**
   * Calculate Iron Fly metrics.
   *
   * @param strikes List of option data at each strike
   * @param context Market and analytics context
   * @return FlyResult with complete fly analysis
   */
  public static FlyResult calculateIronFly(List<OptionStrikeData> strikes,
                                           FlyContext context) {
    if (strikes == null || strikes.isEmpty()) {
      return FlyResult.builder().build();
    }

    // Step 1: Construct the fly
    FlyStructure structure = constructIronFly(strikes, context);

    // Step 2: If fly construction failed, try Iron Condor fallback
    boolean usedCondor = false;
    FlyStructure condorStructure = null;
    if (structure == null) {
      condorStructure = constructIronCondor(strikes, context);
      if (condorStructure != null) {
        usedCondor = true;
        structure = condorStructure;
      }
    }

    if (structure == null) {
      return FlyResult.builder()
          .urgencyRating("NO_TRADE")
          .build();
    }

    // Step 3: Calculate edge metrics
    Double flyCenterScore = calculateFlyCenterScore(structure, context);
    Double wingRichnessScore = calculateWingRichnessScore(structure, context);
    Double thetaToMove = calculateThetaToMove(structure, context);
    Double bepg = calculateBepg(structure, context);
    Double cpr = calculateCpr(structure);
    Double mrb = calculateMrb(structure, context);

    // Step 4: Calculate conviction score
    Double conviction = calculateFlyConviction(flyCenterScore, wingRichnessScore,
        thetaToMove, bepg, cpr, mrb);

    // Step 5: Determine urgency
    String urgency = determineUrgency(structure, context, conviction);

    return FlyResult.builder()
        .structure(structure)
        .flyCenterScore(flyCenterScore)
        .wingRichnessScore(wingRichnessScore)
        .thetaToMove(thetaToMove)
        .bepg(bepg)
        .cpr(cpr)
        .mrb(mrb)
        .flyConviction(conviction)
        .urgencyRating(urgency)
        .usedIronCondor(usedCondor)
        .condorStructure(usedCondor ? condorStructure : null)
        .build();
  }

  /**
   * Construct Iron Fly near forward price.
   * - Center selection near forward price
   * - Wing-widen loop (max 3 iterations)
   */
  private static FlyStructure constructIronFly(List<OptionStrikeData> strikes,
                                                FlyContext context) {
    // Find ATM strikes (near forward)
    OptionStrikeData atmStrike = findNearestStrike(strikes, context.getForwardPrice());
    if (atmStrike == null) return null;

    double centerStrike = atmStrike.getStrike();
    double spot = context.getSpot();

    // Initial wing width: ~2-3% of spot
    double initialWidth = spot * 0.025;
    double wingWidth = initialWidth;

    FlyStructure bestStructure = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    // Wing-widen loop (max 3 iterations)
    for (int i = 0; i < MAX_WING_WIDEN_ITERATIONS; i++) {
      double longCallStrike = centerStrike + wingWidth;
      double longPutStrike = centerStrike - wingWidth;

      OptionStrikeData callWing = findNearestStrike(strikes, longCallStrike);
      OptionStrikeData putWing = findNearestStrike(strikes, longPutStrike);

      if (callWing != null && putWing != null) {
        FlyStructure structure = buildFlyStructure(
            atmStrike, atmStrike, callWing, putWing, context);

        // Score the structure
        double score = scoreStructure(structure, context);

        if (score > bestScore) {
          bestScore = score;
          bestStructure = structure;
        }
      }

      // Widen wings for next iteration
      wingWidth *= 1.5;
    }

    return bestStructure;
  }

  /**
   * Construct Iron Condor (fallback).
   * - Different strikes for short call and short put
   * - Wider profit zone than fly
   */
  private static FlyStructure constructIronCondor(List<OptionStrikeData> strikes,
                                                   FlyContext context) {
    // Find OTM short strikes (slightly away from ATM)
    double spot = context.getSpot();
    double atmWidth = spot * 0.01; // 1% OTM for shorts

    OptionStrikeData shortCall = findNearestStrike(strikes, spot + atmWidth);
    OptionStrikeData shortPut = findNearestStrike(strikes, spot - atmWidth);

    if (shortCall == null || shortPut == null) return null;

    // Long wings further OTM
    double wingWidth = spot * 0.04; // 4% for longs
    OptionStrikeData longCall = findNearestStrike(strikes, spot + wingWidth);
    OptionStrikeData longPut = findNearestStrike(strikes, spot - wingWidth);

    if (longCall == null || longPut == null) return null;

    return buildFlyStructure(shortCall, shortPut, longCall, longPut, context);
  }

  /**
   * Build fly structure from strikes.
   */
  private static FlyStructure buildFlyStructure(OptionStrikeData shortCall,
                                               OptionStrikeData shortPut,
                                               OptionStrikeData longCall,
                                               OptionStrikeData longPut,
                                               FlyContext context) {
    // Calculate premiums (use mid price)
    double callCredit = 0.0;
    if (shortCall.getCallMid() != null && longCall.getCallMid() != null) {
      callCredit = shortCall.getCallMid() - longCall.getCallMid();
    }

    double putCredit = 0.0;
    if (shortPut.getPutMid() != null && longPut.getPutMid() != null) {
      putCredit = shortPut.getPutMid() - longPut.getPutMid();
    }

    double totalCredit = callCredit + putCredit;

    // Max risk = wing width - credit
    double callWingWidth = longCall.getStrike() - shortCall.getStrike();
    double putWingWidth = shortPut.getStrike() - longPut.getStrike();
    double maxRisk = Math.max(callWingWidth, putWingWidth) - totalCredit;

    // Net Greeks
    double netDelta = 0.0;
    double netGamma = 0.0;
    double netTheta = 0.0;
    double netVega = 0.0;

    // Short call
    if (shortCall.getCallDelta() != null) netDelta -= shortCall.getCallDelta();
    if (shortCall.getCallGamma() != null) netGamma -= shortCall.getCallGamma();
    if (shortCall.getCallTheta() != null) netTheta -= shortCall.getCallTheta();
    if (shortCall.getCallVega() != null) netVega -= shortCall.getCallVega();

    // Short put
    if (shortPut.getPutDelta() != null) netDelta -= shortPut.getPutDelta();
    if (shortPut.getPutGamma() != null) netGamma -= shortPut.getPutGamma();
    if (shortPut.getPutTheta() != null) netTheta -= shortPut.getPutTheta();
    if (shortPut.getPutVega() != null) netVega -= shortPut.getPutVega();

    // Long call
    if (longCall.getCallDelta() != null) netDelta += longCall.getCallDelta();
    if (longCall.getCallGamma() != null) netGamma += longCall.getCallGamma();
    if (longCall.getCallTheta() != null) netTheta += longCall.getCallTheta();
    if (longCall.getCallVega() != null) netVega += longCall.getCallVega();

    // Long put
    if (longPut.getPutDelta() != null) netDelta += longPut.getPutDelta();
    if (longPut.getPutGamma() != null) netGamma += longPut.getPutGamma();
    if (longPut.getPutTheta() != null) netTheta += longPut.getPutTheta();
    if (longPut.getPutVega() != null) netVega += longPut.getPutVega();

    // Breakevens
    double upperBreakeven = shortCall.getStrike() + totalCredit;
    double lowerBreakeven = shortPut.getStrike() - totalCredit;

    double profitZone = upperBreakeven - lowerBreakeven;
    double profitZonePct = profitZone / context.getSpot() * 100.0;

    return FlyStructure.builder()
        .shortCallStrike(shortCall.getStrike())
        .shortPutStrike(shortPut.getStrike())
        .longCallStrike(longCall.getStrike())
        .longPutStrike(longPut.getStrike())
        .centerStrike((shortCall.getStrike() + shortPut.getStrike()) / 2.0)
        .wingWidth((longCall.getStrike() - shortCall.getStrike()))
        .callCredit(callCredit)
        .putCredit(putCredit)
        .totalCredit(totalCredit)
        .maxRisk(maxRisk)
        .netDelta(netDelta)
        .netGamma(netGamma)
        .netTheta(netTheta)
        .netVega(netVega)
        .upperBreakeven(upperBreakeven)
        .lowerBreakeven(lowerBreakeven)
        .profitZoneWidth(profitZone)
        .profitZonePercent(profitZonePct)
        .build();
  }

  /**
   * Score a structure (higher is better).
   */
  private static double scoreStructure(FlyStructure structure, FlyContext context) {
    double score = 0.0;

    // Prefer structures with reasonable profit zone
    if (structure.getProfitZonePercent() != null) {
      if (structure.getProfitZonePercent() >= 2.0 && structure.getProfitZonePercent() <= 5.0) {
        score += 10;
      }
    }

    // Prefer positive theta
    if (structure.getNetTheta() > 0) {
      score += structure.getNetTheta() * 100; // Scale up
    }

    // Prefer low delta (neutral)
    if (structure.getNetDelta() != null) {
      score -= Math.abs(structure.getNetDelta()) * 10;
    }

    return score;
  }

  /**
   * Calculate Fly Center Score (PinIndex + proximity + Gamma Tilt + OI surge).
   */
  private static Double calculateFlyCenterScore(FlyStructure structure, FlyContext context) {
    double score = 0.0;
    int components = 0;

    // Pin Index component
    if (context.getOiResult() != null && context.getOiResult().getPinRiskIndex() != null) {
      score += context.getOiResult().getPinRiskIndex() * 25;
      components++;
    }

    // Proximity to max gamma strike
    if (context.getGexResult() != null && context.getGexResult().getMaxGammaStrike() != null) {
      double distance = Math.abs(structure.getCenterStrike()
          - context.getGexResult().getMaxGammaStrike());
      double distancePct = distance / context.getSpot() * 100.0;
      score += Math.max(0, 25 - distancePct); // Closer is better
      components++;
    }

    // Gamma Tilt
    if (context.getGexResult() != null && context.getGexResult().getNetAtmGammaTilt() != null) {
      // Prefer neutral tilt
      double tiltNorm = Math.abs(context.getGexResult().getNetAtmGammaTilt())
          / (Math.abs(context.getGexResult().getTotalGex()) + 1e-10);
      score += (1.0 - tiltNorm) * 25;
      components++;
    }

    // OI surge near center
    if (context.getOiResult() != null && context.getOiResult().getOiWalls() != null) {
      for (OpenInterestCalculator.OiWall wall : context.getOiResult().getOiWalls()) {
        if (Math.abs(wall.getStrike() - structure.getCenterStrike())< context.getSpot() * 0.02) {
          score += Math.min(25, wall.getRatioToSma() * 2);
          components++;
          break;
        }
      }
    }

    return components > 0 ? score / components * 4 : 50.0; // Normalize to ~0-100
  }

  /**
   * Calculate Wing Richness Score.
   * Higher when wings are relatively cheap (good for buying protection).
   */
  private static Double calculateWingRichnessScore(FlyStructure structure,
                                                    FlyContext context) {
    if (context.getVolSurface() == null
        || context.getVolSurface().getButterfly25Delta() == null) {
      return 50.0;
    }

    // Butterfly25Delta represents how rich wings are
    double bf25 = context.getVolSurface().getButterfly25Delta();

    // Lower BF25 = cheaper wings = better for buying (higher score)
    // Scale: 0 BF25 = 100 score, 0.05 BF25 = 0 score
    return Math.max(0, 100 - bf25 * 2000);
  }

  /**
   * Calculate Theta-to-Move ratio.
   * T/M = Daily Theta / Expected Daily Move
   */
  private static Double calculateThetaToMove(FlyStructure structure, FlyContext context) {
    if (structure.getNetTheta() == null || context.getAtTheMoneyImpliedVolatility() <= 0) return null;

    double dailyTheta = Math.abs(structure.getNetTheta());
    double expectedMove = context.getSpot() * context.getAtTheMoneyImpliedVolatility() / Math.sqrt(252.0);

    return dailyTheta / expectedMove;
  }

  /**
   * Calculate Breach Probability (BEPG).
   * Probability of touching breakeven before expiry.
   */
  private static Double calculateBepg(FlyStructure structure, FlyContext context) {
    if (structure.getProfitZonePercent() == null || context.getAtTheMoneyImpliedVolatility() <= 0) return null;

    // Simplified: probability decreases with wider profit zone and lower IV
    double profitZone = structure.getProfitZonePercent() / 100.0;
    double iv = context.getAtTheMoneyImpliedVolatility();

    // Approximation: BEPG = 1 - (profit zone / expected range)
    double expectedRange = iv * Math.sqrt(context.getDaysToExpiry() / 252.0);
    double bepg = 1.0 - (profitZone / (expectedRange + 1e-10));

    return Math.max(0, Math.min(1, bepg));
  }

  /**
   * Calculate Credit-per-Risk (CpR).
   * CpR = Total Credit / Max Risk
   */
  private static Double calculateCpr(FlyStructure structure) {
    if (structure.getTotalCredit() <= 0 || structure.getMaxRisk() <= 0) return null;

    return structure.getTotalCredit() / structure.getMaxRisk();
  }

  /**
   * Calculate Magnet-Risk Balance (MRB).
   * Balances gamma magnet proximity vs breach risk.
   */
  private static Double calculateMrb(FlyStructure structure, FlyContext context) {
    double score = 50.0; // Neutral start

    // Bonus for being near gamma magnet
    if (context.getGexResult() != null && context.getGexResult().getMaxGammaStrike() != null) {
      double dist = Math.abs(structure.getCenterStrike()
          - context.getGexResult().getMaxGammaStrike());
      double distPct = dist / context.getSpot() * 100.0;
      score += Math.max(0, 20 - distPct);
    }

    // Penalty for high breach probability
    Double bepg = calculateBepg(structure, context);
    if (bepg != null) {
      score -= bepg * 30; // Up to 30 point penalty
    }

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Calculate Fly Conviction composite score.
   */
  private static Double calculateFlyConviction(Double flyCenterScore,
                                                Double wingRichnessScore,
                                                Double thetaToMove,
                                                Double bepg,
                                                Double cpr,
                                                Double mrb) {
    double score = 0.0;
    int count = 0;

    if (flyCenterScore != null) {
      score += flyCenterScore * 0.25;
      count++;
    }
    if (wingRichnessScore != null) {
      score += wingRichnessScore * 0.20;
      count++;
    }
    if (thetaToMove != null && thetaToMove > 0) {
      score += Math.min(100, thetaToMove * 100) * 0.20;
      count++;
    }
    if (bepg != null) {
      score += (1.0 - bepg) * 100 * 0.15; // Lower BEPG is better
      count++;
    }
    if (cpr != null) {
      score += Math.min(100, cpr * 100) * 0.10;
      count++;
    }
    if (mrb != null) {
      score += mrb * 0.10;
      count++;
    }

    return count > 0 ? score : null;
  }

  /**
   * Determine urgency rating.
   */
  private static String determineUrgency(FlyStructure structure, FlyContext context,
                                          Double conviction) {
    if (conviction == null) return "NO_TRADE";
    if (conviction >= 75) return "HIGH";
    if (conviction >= 50) return "MEDIUM";
    if (conviction >= 25) return "LOW";
    return "NO_TRADE";
  }

  /**
   * Find nearest strike to target price.
   */
  private static OptionStrikeData findNearestStrike(List<OptionStrikeData> strikes,
                                                    double target) {
    return strikes.stream()
        .min(Comparator.comparing(s -> Math.abs(s.getStrike() - target)))
        .orElse(null);
  }
}
