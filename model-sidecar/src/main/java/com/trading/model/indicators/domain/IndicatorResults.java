package com.trading.model.indicators.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All indicator results from the indicators engine.
 * This aggregates outputs from all 11 ThinkScript indicators.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorResults {
    
    // ---------- Volume Indicators ----------
    private Double volumeStrength;           // VR (volume ratio) with RTH projection
    private Double avgVolume;              // Average volume over lookback period
    
    // ---------- Volatility Indicators ----------
    private Double impliedVolatility;        // Current IV
    private Double ivRank;                   // IV Rank (percentile)
    private Double ivHvRatio;              // IV/HV ratio
    private Double volatilityRiskPremium;    // VRP = IV - HV
    private Double hv30Percentile;          // Historical vol percentile over rankLen
    private Double atmVega30Day;             // 30-day ATM vega
    private Double atmVegaPercentile;        // ATM vega percentile
    
    // ---------- Correlation ----------
    private Double spxCorrelation;           // 30-day correlation with SPX
    
    // ---------- Momentum/Trend ----------
    private TrendDirection trendDirection;   // Classification based on MACD, RSI, ADX
    private Double macdLine;                 // MACD line value
    private Double macdSignal;               // MACD signal line
    private Double rsiValue;                 // RSI value
    private Double adxValue;                 // ADX value
    
    // ---------- Condor Score ----------
    private Double condorScore;              // Proximity + ADX + IV/HV composite
    private CondorSignal condorSignal;       // Signal classification
    
    // ---------- Drift Efficiency ----------
    private Double driftEfficiency;          // (close-open)/open vs range
    private DriftEfficiencyClass driftClass; // Classification
    
    // ---------- Technical Indicators ----------
    private Double atr14;                    // 14-period ATR
    private VWAPResult vwap;                 // VWAP with 1.5 std dev bands
    private BollingerBandResult bollingerBands; // Standard BB (20, 2.0)
    
    /**
     * Trend direction enumeration based on MACD, RSI, ADX analysis
     */
    public enum TrendDirection {
        STRONG_UP,
        EXPL_DN,
        FADE,
        PULL_REC,
        FLAT,
        MIXED
    }
    
    /**
     * Condor signal classification
     */
    public enum CondorSignal {
        BULL_CREDIT,
        BEAR_CREDIT,
        BULL_DEBIT,
        BEAR_DEBIT,
        CONDOR,
        WATCH
    }
    
    /**
     * Drift efficiency classification
     */
    public enum DriftEfficiencyClass {
        HIGH_EFFICIENCY,
        MODERATE_EFFICIENCY,
        LOW_EFFICIENCY
    }
    
    /**
     * VWAP with bands result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VWAPResult {
        private Double vwap;       // VWAP value
        private Double upperBand;  // Upper band (VWAP + 1.5 * std dev)
        private Double lowerBand;  // Lower band (VWAP - 1.5 * std dev)
    }
    
    /**
     * Bollinger Bands result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BollingerBandResult {
        private Double upper;    // Upper band (SMA + 2.0 * std dev)
        private Double middle;   // Middle band (20-period SMA)
        private Double lower;    // Lower band (SMA - 2.0 * std dev)
    }
}
