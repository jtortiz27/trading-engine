package com.trading.model.indicators.service;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;
import com.trading.model.indicators.engine.IndicatorsEngine;
import com.trading.model.indicators.engine.MomentumIndicators;
import com.trading.model.indicators.engine.TechnicalIndicators;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * IndicatorsService - Aggregates all ThinkScript indicator calculations.
 * 
 * This service provides a unified interface for calculating all indicators
 * from the Ultra Strategy Dashboard v0.5.
 * 
 * ThinkScript → Java Mapping Summary:
 * 1. VolumeStrength: VR (volume ratio) with RTH time projection
 * 2. IV Dashboard: IV, IV-Rank, IV/HV ratio, VRP
 * 3. HV30 Percentile: Historical vol percentile over rankLen
 * 4. Correlation with SPX: 30-day correlation
 * 5. ATM Vega: 30-day ATM vega calculation with percentile
 * 6. Momentum/Trend Arrows: MACD, RSI, ADX-based trend taxonomy
 * 7. CondorScore: Proximity + ADX + IV/HV composite
 * 8. Drift Efficiency: (close-open)/open vs range
 * 9. ATR14: Standard ATR
 * 10. VWAP + Bands: VWAP with 1.5 std dev bands
 * 11. Bollinger Bands: Standard BB (20, 2.0)
 */
@Service
public class IndicatorsService {

    // Default parameters matching ThinkScript inputs
    private static final int DEFAULT_VOL_LOOKBACK = 20;
    private static final int DEFAULT_HV_LEN = 30;
    private static final int DEFAULT_RANK_LEN = 252;
    private static final int DEFAULT_CORR_LEN = 30;
    private static final int DEFAULT_ATR_LEN = 14;
    private static final int DEFAULT_BB_LEN = 20;
    private static final double DEFAULT_BB_DEV = 2.0;
    private static final double DEFAULT_VWAP_DEV = 1.5;
    private static final double DEFAULT_IVRICH_THRESHOLD = 1.2;
    private static final double DEFAULT_DRIFT_HI = 0.6;
    private static final double DEFAULT_DRIFT_LO = 0.2;
    private static final double ANNUALIZATION_FACTOR = 252.0;
    private static final double RISK_FREE_RATE = 0.04;

    /**
     * Calculates all indicators for a given set of price bars.
     * 
     * @param bars OHLCV bars for the primary symbol (most recent first)
     * @param spxBars OHLCV bars for SPX correlation (most recent first)
     * @param ivHistory Historical IV values for IV Rank calculation
     * @param hvHistory Historical HV values for HV Percentile
     * @param vegaHistory Historical vega values for Vega Percentile
     * @param currentIV Current implied volatility
     * @param currentHV Current historical volatility
     * @param rthMinutesElapsed Minutes elapsed in RTH session
     * @param rthTotalMinutes Total RTH minutes (e.g., 390 for 9:30-16:00)
     * @return Complete IndicatorResults with all calculated values
     */
    public IndicatorResults calculateAllIndicators(
            List<OHLCV> bars,
            List<OHLCV> spxBars,
            List<Double> ivHistory,
            List<Double> hvHistory,
            List<Double> vegaHistory,
            Double currentIV,
            Double currentHV,
            double rthMinutesElapsed,
            double rthTotalMinutes) {
        
        if (bars == null || bars.isEmpty()) {
            return null;
        }
        
        IndicatorResults.IndicatorResultsBuilder builder = IndicatorResults.builder();
        
        // ---------- Volume Indicators ----------
        builder.volumeStrength(IndicatorsEngine.calculateVolumeStrength(
            bars, DEFAULT_VOL_LOOKBACK, rthMinutesElapsed, rthTotalMinutes));
        
        // ---------- Volatility Indicators ----------
        if (currentIV != null && ivHistory != null) {
            builder.impliedVolatility(currentIV);
            builder.ivRank(IndicatorsEngine.calculateIVRank(currentIV, ivHistory, DEFAULT_RANK_LEN));
        }
        
        if (currentIV != null && currentHV != null) {
            builder.ivHvRatio(IndicatorsEngine.calculateIVHVRatio(currentIV, currentHV));
            builder.volatilityRiskPremium(IndicatorsEngine.calculateVRP(currentIV, currentHV));
        }
        
        builder.hv30Percentile(IndicatorsEngine.calculateHVPercentile(
            currentHV != null ? currentHV : 0, hvHistory, DEFAULT_RANK_LEN));
        
        // ---------- Correlation ----------
        if (spxBars != null) {
            builder.spxCorrelation(IndicatorsEngine.calculateSPXCorrelation(
                bars, spxBars, DEFAULT_CORR_LEN));
        }
        
        // ---------- ATM Vega ----------
        if (!bars.isEmpty() && currentIV != null) {
            OHLCV currentBar = bars.get(0);
            double atmVega = IndicatorsEngine.calculateATMVega(
                currentBar.getClose(),
                currentBar.getClose(), // ATM strike
                30.0 / 365.0, // 30 days
                currentIV / 100.0, // Convert to decimal
                RISK_FREE_RATE);
            builder.atmVega30Day(atmVega);
            
            if (vegaHistory != null && atmVega != null) {
                builder.atmVegaPercentile(IndicatorsEngine.calculateVegaPercentile(
                    atmVega, vegaHistory, DEFAULT_RANK_LEN));
            }
        }
        
        // ---------- Momentum/Trend ----------
        MomentumIndicators.MACDResult macd = MomentumIndicators.calculateMACD(
            bars, 12, 26, 9);
        Double rsi = MomentumIndicators.calculateRSI(bars, 14);
        MomentumIndicators.ADXResult adx = MomentumIndicators.calculateADX(bars, 14);
        
        if (macd != null) {
            builder.macdLine(macd.getMacdLine());
            builder.macdSignal(macd.getSignalLine());
        }
        builder.rsiValue(rsi);
        if (adx != null) {
            builder.adxValue(adx.getAdx());
        }
        builder.trendDirection(MomentumIndicators.classifyTrend(macd, rsi, adx));
        
        // ---------- Condor Score ----------
        IndicatorResults.BollingerBandResult bb = TechnicalIndicators.calculateBollingerBands(
            bars, DEFAULT_BB_LEN, DEFAULT_BB_DEV);
        builder.bollingerBands(bb);
        
        if (bb != null && !bars.isEmpty() && adx != null) {
            Double ivHvRatio = builder.build().getIvHvRatio();
            MomentumIndicators.CondorScoreResult condor = MomentumIndicators.calculateCondorScore(
                bars.get(0).getClose(),
                bb.getUpper(),
                bb.getLower(),
                bb.getMiddle(),
                adx.getAdx(),
                ivHvRatio,
                DEFAULT_IVRICH_THRESHOLD);
            builder.condorScore(condor.getScore());
            builder.condorSignal(condor.getSignal());
        }
        
        // ---------- Drift Efficiency ----------
        if (!bars.isEmpty()) {
            MomentumIndicators.DriftEfficiencyResult drift = MomentumIndicators.calculateDriftEfficiency(
                bars.get(0), DEFAULT_DRIFT_HI, DEFAULT_DRIFT_LO);
            if (drift != null) {
                builder.driftEfficiency(drift.getEfficiency());
                builder.driftClass(drift.getDriftClass());
            }
        }
        
        // ---------- Technical Indicators ----------
        builder.atr14(TechnicalIndicators.calculateATR(bars, DEFAULT_ATR_LEN));
        
        // VWAP (use intraday bars if available, otherwise use all)
        builder.vwap(TechnicalIndicators.calculateVWAP(bars, DEFAULT_VWAP_DEV));
        
        return builder.build();
    }
    
    /**
     * Simplified method for calculating indicators with minimal inputs.
     * Uses default values for optional parameters.
     * 
     * @param bars OHLCV bars (most recent first)
     * @return IndicatorResults with available calculations
     */
    public IndicatorResults calculateBasicIndicators(List<OHLCV> bars) {
        return calculateAllIndicators(bars, null, null, null, null, null, null, 195, 390);
    }
    
    // ============================================
    // Individual Indicator Methods
    // ============================================
    
    public Double calculateVolumeStrength(List<OHLCV> bars, double rthMinutesElapsed, double rthTotalMinutes) {
        return IndicatorsEngine.calculateVolumeStrength(bars, DEFAULT_VOL_LOOKBACK, rthMinutesElapsed, rthTotalMinutes);
    }
    
    public Double calculateATR14(List<OHLCV> bars) {
        return TechnicalIndicators.calculateATR14(bars);
    }
    
    public IndicatorResults.BollingerBandResult calculateBollingerBands(List<OHLCV> bars) {
        return TechnicalIndicators.calculateBollingerBands(bars);
    }
    
    public IndicatorResults.VWAPResult calculateVWAP(List<OHLCV> bars) {
        return TechnicalIndicators.calculateVWAP(bars);
    }
    
    public MomentumIndicators.MACDResult calculateMACD(List<OHLCV> bars) {
        return MomentumIndicators.calculateMACD(bars, 12, 26, 9);
    }
    
    public Double calculateRSI(List<OHLCV> bars) {
        return MomentumIndicators.calculateRSI(bars, 14);
    }
    
    public MomentumIndicators.ADXResult calculateADX(List<OHLCV> bars) {
        return MomentumIndicators.calculateADX(bars, 14);
    }
}
