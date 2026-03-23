package com.trading.model.indicators.engine;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;

import java.util.List;

/**
 * Technical Indicators: ATR, VWAP, Bollinger Bands.
 * 
 * ThinkScript → Java Mapping:
 * - ATR: Average(TrueRange(high, close, low), length)
 * - VWAP: Sum(volume * (high + low + close) / 3) / Sum(volume)
 * - Bollinger Bands: BollingerBands(close, length, numDev)
 */
public class TechnicalIndicators {

    // ============================================
    // 9. ATR14 - Standard ATR
    // ============================================
    
    /**
     * Calculates Average True Range (ATR).
     * 
     * ThinkScript:
     * def tr = TrueRange(high, close, low);
     * def atr = Average(tr, length);
     * 
     * True Range = Max(High - Low, |High - Close[1]|, |Low - Close[1]|)
     * 
     * @param bars OHLCV bars (most recent first)
     * @param length ATR period (default: 14)
     * @return ATR value
     */
    public static Double calculateATR(List<OHLCV> bars, int length) {
        if (bars == null || bars.size() < length + 1) {
            return null;
        }
        
        double[] trueRanges = new double[length];
        
        for (int i = 0; i < length; i++) {
            double high = bars.get(i).getHigh();
            double low = bars.get(i).getLow();
            double prevClose = bars.get(i + 1).getClose();
            
            // True Range = Max(High - Low, |High - Close[1]|, |Low - Close[1]|)
            double range1 = high - low;
            double range2 = Math.abs(high - prevClose);
            double range3 = Math.abs(low - prevClose);
            
            trueRanges[i] = Math.max(range1, Math.max(range2, range3));
        }
        
        // Use Wilder's smoothing for ATR (standard)
        return wilderSmooth(trueRanges, length);
    }
    
    /**
     * Convenience method for standard 14-period ATR.
     */
    public static Double calculateATR14(List<OHLCV> bars) {
        return calculateATR(bars, 14);
    }

    // ============================================
    // 10. VWAP + Bands - VWAP with 1.5 std dev bands
    // ============================================
    
    /**
     * Calculates VWAP (Volume Weighted Average Price) with standard deviation bands.
     * 
     * ThinkScript:
     * def typicalPrice = (high + low + close) / 3;
     * def vwap = Sum(volume * typicalPrice) / Sum(volume);
     * def variance = Sum(volume * Sqr(typicalPrice - vwap)) / Sum(volume);
     * def stdDev = Sqrt(variance);
     * def upperBand = vwap + numDev * stdDev;
     * def lowerBand = vwap - numDev * stdDev;
     * 
     * @param bars OHLCV bars for VWAP calculation (typically intraday)
     * @param numDev Number of standard deviations (default: 1.5)
     * @return VWAPResult with VWAP and bands
     */
    public static IndicatorResults.VWAPResult calculateVWAP(List<OHLCV> bars, double numDev) {
        if (bars == null || bars.isEmpty()) {
            return null;
        }
        
        double cumulativeTPV = 0.0; // Typical Price * Volume
        double cumulativeVolume = 0.0;
        
        // First pass: Calculate VWAP
        for (OHLCV bar : bars) {
            double typicalPrice = (bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0;
            cumulativeTPV += typicalPrice * bar.getVolume();
            cumulativeVolume += bar.getVolume();
        }
        
        if (cumulativeVolume == 0) {
            return null;
        }
        
        double vwap = cumulativeTPV / cumulativeVolume;
        
        // Second pass: Calculate variance
        double sumWeightedSquaredDiff = 0.0;
        for (OHLCV bar : bars) {
            double typicalPrice = (bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0;
            double diff = typicalPrice - vwap;
            sumWeightedSquaredDiff += bar.getVolume() * diff * diff;
        }
        
        double variance = sumWeightedSquaredDiff / cumulativeVolume;
        double stdDev = Math.sqrt(variance);
        
        double upperBand = vwap + numDev * stdDev;
        double lowerBand = vwap - numDev * stdDev;
        
        return IndicatorResults.VWAPResult.builder()
                .vwap(vwap)
                .upperBand(upperBand)
                .lowerBand(lowerBand)
                .build();
    }
    
    /**
     * Standard VWAP with 1.5 standard deviation bands.
     */
    public static IndicatorResults.VWAPResult calculateVWAP(List<OHLCV> bars) {
        return calculateVWAP(bars, 1.5);
    }

    // ============================================
    // 11. Bollinger Bands - Standard BB (20, 2.0)
    // ============================================
    
    /**
     * Calculates Bollinger Bands.
     * 
     * ThinkScript:
     * def basis = Average(close, length);  // Middle band = SMA
     * def dev = numDev * StDev(close, length);  // numDev standard deviations
     * def upper = basis + dev;
     * def lower = basis - dev;
     * 
     * @param bars OHLCV bars (most recent first)
     * @param length SMA period (default: 20)
     * @param numDev Number of standard deviations (default: 2.0)
     * @return BollingerBandResult with upper, middle, and lower bands
     */
    public static IndicatorResults.BollingerBandResult calculateBollingerBands(
            List<OHLCV> bars, 
            int length, 
            double numDev) {
        
        if (bars == null || bars.size() < length) {
            return null;
        }
        
        // Calculate SMA (middle band)
        double sma = 0.0;
        for (int i = 0; i < length; i++) {
            sma += bars.get(i).getClose();
        }
        sma /= length;
        
        // Calculate standard deviation
        double sumSquaredDiff = 0.0;
        for (int i = 0; i < length; i++) {
            double diff = bars.get(i).getClose() - sma;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / length);
        
        double upper = sma + numDev * stdDev;
        double lower = sma - numDev * stdDev;
        
        return IndicatorResults.BollingerBandResult.builder()
                .upper(upper)
                .middle(sma)
                .lower(lower)
                .build();
    }
    
    /**
     * Standard Bollinger Bands with period 20 and 2.0 deviations.
     */
    public static IndicatorResults.BollingerBandResult calculateBollingerBands(List<OHLCV> bars) {
        return calculateBollingerBands(bars, 20, 2.0);
    }

    // ============================================
    // Helper methods
    // ============================================
    
    /**
     * Wilder's smoothing method (used in ATR).
     * Equivalent to EMA with alpha = 1/length.
     */
    private static double wilderSmooth(double[] data, int length) {
        if (data.length == 0) return 0;
        
        double alpha = 1.0 / length;
        double smoothed = data[data.length - 1];
        
        for (int i = data.length - 2; i >= 0; i--) {
            smoothed = data[i] * alpha + smoothed * (1 - alpha);
        }
        
        return smoothed;
    }
}
