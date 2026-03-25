package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.CondorScore;
import java.util.List;

/**
 * Condor Score Calculator.
 * Composite indicator for iron condor strategy suitability.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def range = Highest(high, length) - Lowest(low, length);
 * def midPoint = (Highest(high, length) + Lowest(low, length)) / 2;
 * def proximity = 100 - AbsValue(close - midPoint) / range * 100;
 * def adxComponent = Max(0, 100 - ADX(adxLength));
 * def ivHvComponent = ivHvRatio * 50;
 * def condorScore = (proximity * 0.4 + adxComponent * 0.3 + ivHvComponent * 0.3);
 * ```
 */
public class CondorScoreCalculator implements IndicatorCalculator<CondorScoreCalculator.CondorInput, CondorScore> {

  private final int length;
  private final int adxLength;
  private final double favorableThreshold;
  private final double strongThreshold;

  // Component weights
  private final double proximityWeight;
  private final double adxWeight;
  private final double ivHvWeight;

  public CondorScoreCalculator() {
    this(20, 14, 60.0, 75.0, 0.4, 0.3, 0.3);
  }

  /**
   * @param length Lookback for range calculation
   * @param adxLength ADX calculation length
   * @param favorableThreshold Score threshold for "favorable" (default 60)
   * @param strongThreshold Score threshold for "strong" (default 75)
   * @param proximityWeight Weight for proximity component (default 0.4)
   * @param adxWeight Weight for ADX component (default 0.3)
   * @param ivHvWeight Weight for IV/HV component (default 0.3)
   */
  public CondorScoreCalculator(int length, int adxLength,
                                double favorableThreshold, double strongThreshold,
                                double proximityWeight, double adxWeight, double ivHvWeight) {
    this.length = length;
    this.adxLength = adxLength;
    this.favorableThreshold = favorableThreshold;
    this.strongThreshold = strongThreshold;
    this.proximityWeight = proximityWeight;
    this.adxWeight = adxWeight;
    this.ivHvWeight = ivHvWeight;
  }

  @Override
  public CondorScore calculate(CondorInput input) {
    List<Bar> bars = input.getBars();
    double ivHvRatio = input.getIvHvRatio();
    double adx = input.getAdx();

    if (bars == null || bars.size() < length) {
      return CondorScore.builder()
          .score(50.0)
          .proximityComponent(50.0)
          .adxComponent(50.0)
          .ivHvComponent(50.0)
          .isFavorable(false)
          .recommendation(CondorScore.CondorRecommendation.NEUTRAL)
          .build();
    }

    // Calculate proximity component
    double proximity = calculateProximity(bars);

    // Calculate ADX component (low ADX is better for condor)
    double adxComponent = Math.max(0, 100 - adx);

    // Calculate IV/HV component (elevated IV is better for condor)
    double ivHvComponent = Math.min(100, ivHvRatio * 50);

    // Composite score
    double score = proximity * proximityWeight
        + adxComponent * adxWeight
        + ivHvComponent * ivHvWeight;

    // Determine recommendation
    CondorScore.CondorRecommendation recommendation;
    if (score >= strongThreshold) {
      recommendation = CondorScore.CondorRecommendation.STRONG_CONDOR;
    } else if (score >= favorableThreshold) {
      recommendation = CondorScore.CondorRecommendation.MODERATE_CONDOR;
    } else if (score >= 40) {
      recommendation = CondorScore.CondorRecommendation.NEUTRAL;
    } else {
      recommendation = CondorScore.CondorRecommendation.AVOID;
    }

    return CondorScore.builder()
        .score(score)
        .proximityComponent(proximity)
        .adxComponent(adxComponent)
        .ivHvComponent(ivHvComponent)
        .isFavorable(score >= favorableThreshold)
        .recommendation(recommendation)
        .build();
  }

  /**
   * Calculate proximity: how close price is to middle of range.
   * ThinkScript: 100 - AbsValue(close - midPoint) / range * 100
   */
  private double calculateProximity(List<Bar> bars) {
    double highestHigh = Double.MIN_VALUE;
    double lowestLow = Double.MAX_VALUE;

    int startIdx = bars.size() - length;
    for (int i = startIdx; i < bars.size(); i++) {
      highestHigh = Math.max(highestHigh, bars.get(i).getHigh());
      lowestLow = Math.min(lowestLow, bars.get(i).getLow());
    }

    double range = highestHigh - lowestLow;
    if (range == 0) {
      return 100.0; // Perfect proximity if no range
    }

    double midPoint = (highestHigh + lowestLow) / 2.0;
    double currentClose = bars.get(bars.size() - 1).getClose();

    return 100.0 - (Math.abs(currentClose - midPoint) / range * 100.0);
  }

  @Override
  public int getRequiredDataPoints() {
    return Math.max(length, adxLength * 2);
  }

  /**
   * Input wrapper for Condor Score calculation.
   */
  public static class CondorInput {
    private final List<Bar> bars;
    private final double ivHvRatio;
    private final double adx;

    public CondorInput(List<Bar> bars, double ivHvRatio, double adx) {
      this.bars = bars;
      this.ivHvRatio = ivHvRatio;
      this.adx = adx;
    }

    public List<Bar> getBars() {
      return bars;
    }

    public double getIvHvRatio() {
      return ivHvRatio;
    }

    public double getAdx() {
      return adx;
    }
  }
}
