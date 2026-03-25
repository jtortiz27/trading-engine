package com.trading.indicators.service;

import com.trading.indicators.calculators.*;
import com.trading.indicators.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for calculating all ThinkScript-ported indicators.
 * Provides a unified interface for indicator calculations.
 *
 * ThinkScript Ultra Strategy Dashboard v0.5 - Java Port
 */
@Service
public class IndicatorService {

  private final VolumeStrengthCalculator volumeStrengthCalculator;
  private final IVDashboardCalculator ivDashboardCalculator;
  private final HVPercentileCalculator hvPercentileCalculator;
  private final CorrelationCalculator correlationCalculator;
  private final ATMVegaCalculator atmVegaCalculator;
  private final MomentumTrendArrowsCalculator momentumCalculator;
  private final CondorScoreCalculator condorScoreCalculator;
  private final DriftEfficiencyCalculator driftEfficiencyCalculator;
  private final ATR14Calculator atr14Calculator;
  private final VWAPBandsCalculator vwapBandsCalculator;
  private final BollingerBandsCalculator bollingerBandsCalculator;

  public IndicatorService() {
    // Default configurations
    this.volumeStrengthCalculator = new VolumeStrengthCalculator();
    this.ivDashboardCalculator = new IVDashboardCalculator(30, 252);
    this.hvPercentileCalculator = new HVPercentileCalculator();
    this.correlationCalculator = new CorrelationCalculator();
    this.atmVegaCalculator = new ATMVegaCalculator();
    this.momentumCalculator = new MomentumTrendArrowsCalculator();
    this.condorScoreCalculator = new CondorScoreCalculator();
    this.driftEfficiencyCalculator = new DriftEfficiencyCalculator();
    this.atr14Calculator = new ATR14Calculator();
    this.vwapBandsCalculator = new VWAPBandsCalculator();
    this.bollingerBandsCalculator = new BollingerBandsCalculator();
  }

  /**
   * Calculate Volume Strength indicator.
   * ThinkScript: VolumeRatio with RTH projection
   */
  public VolumeStrength calculateVolumeStrength(List<Bar> bars) {
    return volumeStrengthCalculator.calculate(bars);
  }

  /**
   * Calculate IV Dashboard.
   * ThinkScript: IV, IV-Rank, IV/HV, VRP
   */
  public IVDashboard calculateIVDashboard(List<Bar> bars, List<Double> ivHistory) {
    IVDashboardCalculator calculator = new IVDashboardCalculator(30, 252, ivHistory);
    return calculator.calculate(bars);
  }

  /**
   * Calculate HV30 Percentile.
   * ThinkScript: historical_volatility(30) percentile
   */
  public HVPercentile calculateHVPercentile(List<Bar> bars) {
    return hvPercentileCalculator.calculate(bars);
  }

  /**
   * Calculate Correlation with SPX.
   * ThinkScript: Correlation(close, SPX.close, 30)
   */
  public CorrelationResult calculateCorrelation(List<Double> primaryPrices, List<Double> comparisonPrices) {
    CorrelationCalculator.CorrelationInput input =
        new CorrelationCalculator.CorrelationInput(primaryPrices, comparisonPrices);
    return correlationCalculator.calculate(input);
  }

  /**
   * Calculate ATM Vega.
   * ThinkScript: option_vega for ATM strike
   */
  public ATMVega calculateATMVega(double underlyingPrice, double iv, List<Double> vegaHistory) {
    ATMVegaCalculator.ATMVegaInput input =
        new ATMVegaCalculator.ATMVegaInput(underlyingPrice, iv, vegaHistory);
    return atmVegaCalculator.calculate(input);
  }

  /**
   * Calculate Momentum/Trend Arrows.
   * ThinkScript: MACD, RSI, ADX composite
   */
  public MomentumTrendArrows calculateMomentumTrend(List<Bar> bars) {
    return momentumCalculator.calculate(bars);
  }

  /**
   * Calculate Condor Score.
   * ThinkScript: Proximity + ADX + IV/HV composite
   */
  public CondorScore calculateCondorScore(List<Bar> bars, double ivHvRatio, double adx) {
    CondorScoreCalculator.CondorInput input =
        new CondorScoreCalculator.CondorInput(bars, ivHvRatio, adx);
    return condorScoreCalculator.calculate(input);
  }

  /**
   * Calculate Drift Efficiency.
   * ThinkScript: (close - open) / open vs range
   */
  public DriftEfficiency calculateDriftEfficiency(List<Bar> bars) {
    return driftEfficiencyCalculator.calculate(bars);
  }

  /**
   * Calculate ATR14.
   * ThinkScript: Average(TrueRange, 14)
   */
  public ATR14 calculateATR14(List<Bar> bars) {
    return atr14Calculator.calculate(bars);
  }

  /**
   * Calculate VWAP with Bands.
   * ThinkScript: VWAP with 1.5 std dev bands
   */
  public VWAPBands calculateVWAPBands(List<Bar> bars) {
    return vwapBandsCalculator.calculate(bars);
  }

  /**
   * Calculate Bollinger Bands.
   * ThinkScript: BollingerBands(20, 2.0)
   */
  public BollingerBands calculateBollingerBands(List<Bar> bars) {
    return bollingerBandsCalculator.calculate(bars);
  }

  /**
   * Calculate all indicators at once.
   * Returns a comprehensive indicator snapshot.
   */
  public IndicatorSnapshot calculateAllIndicators(
      List<Bar> bars,
      List<Double> ivHistory,
      List<Double> spxPrices,
      List<Double> vegaHistory) {

    double currentPrice = bars.get(bars.size() - 1).getClose();

    // Extract primary prices for correlation
    List<Double> primaryPrices = bars.stream()
        .map(Bar::getClose)
        .toList();

    // Calculate IV Dashboard first to get IV/HV ratio for Condor Score
    IVDashboard ivDashboard = calculateIVDashboard(bars, ivHistory);

    // Calculate Momentum to get ADX for Condor Score
    MomentumTrendArrows momentum = calculateMomentumTrend(bars);

    return IndicatorSnapshot.builder()
        .volumeStrength(calculateVolumeStrength(bars))
        .ivDashboard(ivDashboard)
        .hvPercentile(calculateHVPercentile(bars))
        .correlation(calculateCorrelation(primaryPrices, spxPrices))
        .atmVega(calculateATMVega(currentPrice, ivDashboard.getImpliedVolatility(), vegaHistory))
        .momentumTrend(momentum)
        .condorScore(calculateCondorScore(bars, ivDashboard.getIvHvRatio(), momentum.getAdx()))
        .driftEfficiency(calculateDriftEfficiency(bars))
        .atr14(calculateATR14(bars))
        .vwapBands(calculateVWAPBands(bars))
        .bollingerBands(calculateBollingerBands(bars))
        .build();
  }
}
