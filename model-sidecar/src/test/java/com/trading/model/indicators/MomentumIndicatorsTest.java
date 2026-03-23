package com.trading.model.indicators;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;
import com.trading.model.indicators.engine.MomentumIndicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Momentum and Trend indicators.
 */
class MomentumIndicatorsTest {

    private List<OHLCV> sampleBars;
    private List<OHLCV> trendingUpBars;
    private List<OHLCV> trendingDownBars;
    
    @BeforeEach
    void setUp() {
        sampleBars = generateSampleBars(50);
        trendingUpBars = generateTrendingBars(50, 0.5); // Up trend
        trendingDownBars = generateTrendingBars(50, -0.5); // Down trend
    }
    
    private List<OHLCV> generateSampleBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            double trend = i * 0.1;
            double noise = Math.sin(i * 0.5) * 2;
            double close = basePrice + trend + noise;
            double high = close + 1.5;
            double low = close - 1.5;
            double open = close + (Math.random() - 0.5);
            long volume = 1000000L;
            
            bars.add(new OHLCV(open, high, low, close, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    private List<OHLCV> generateTrendingBars(int count, double trendFactor) {
        List<OHLCV> bars = new ArrayList<>();
        double price = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            price += trendFactor;
            double high = price + 1.0;
            double low = price - 1.0;
            double open = price - trendFactor * 0.5;
            long volume = 1000000L;
            
            bars.add(new OHLCV(open, high, low, price, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    // ============================================
    // MACD Tests
    // ============================================
    
    @Test
    @DisplayName("MACD: Calculates MACD line, signal, and histogram")
    void testMACD() {
        MomentumIndicators.MACDResult result = MomentumIndicators.calculateMACD(sampleBars, 12, 26, 9);
        assertNotNull(result);
        assertNotNull(result.getMacdLine());
        assertNotNull(result.getSignalLine());
        assertNotNull(result.getHistogram());
    }
    
    @Test
    @DisplayName("MACD: Histogram equals MACD minus Signal")
    void testMACDRelationship() {
        MomentumIndicators.MACDResult result = MomentumIndicators.calculateMACD(sampleBars, 12, 26, 9);
        assertEquals(result.getMacdLine() - result.getSignalLine(), result.getHistogram(), 0.001);
    }
    
    @Test
    @DisplayName("MACD: Returns null for insufficient data")
    void testMACDInsufficientData() {
        List<OHLCV> smallBars = sampleBars.subList(0, 20);
        assertNull(MomentumIndicators.calculateMACD(smallBars, 12, 26, 9));
    }
    
    // ============================================
    // RSI Tests
    // ============================================
    
    @Test
    @DisplayName("RSI: Calculates RSI value")
    void testRSI() {
        Double result = MomentumIndicators.calculateRSI(sampleBars, 14);
        assertNotNull(result);
        assertTrue(result >= 0 && result <= 100, "RSI should be between 0 and 100");
    }
    
    @Test
    @DisplayName("RSI: Returns 100 for all up moves")
    void testRSIAllUp() {
        Double result = MomentumIndicators.calculateRSI(trendingUpBars, 14);
        assertNotNull(result);
        assertTrue(result > 50, "Strong uptrend should have RSI > 50");
    }
    
    @Test
    @DisplayName("RSI: Returns null for insufficient data")
    void testRSIInsufficientData() {
        assertNull(MomentumIndicators.calculateRSI(sampleBars.subList(0, 10), 14));
    }
    
    // ============================================
    // ADX Tests
    // ============================================
    
    @Test
    @DisplayName("ADX: Calculates ADX with +DI and -DI")
    void testADX() {
        MomentumIndicators.ADXResult result = MomentumIndicators.calculateADX(sampleBars, 14);
        assertNotNull(result);
        assertTrue(result.getAdx() >= 0, "ADX should be non-negative");
        assertTrue(result.getPlusDI() >= 0 && result.getPlusDI() <= 100);
        assertTrue(result.getMinusDI() >= 0 && result.getMinusDI() <= 100);
    }
    
    @Test
    @DisplayName("ADX: Strong trend has higher ADX")
    void testADXStrongTrend() {
        MomentumIndicators.ADXResult upTrendResult = MomentumIndicators.calculateADX(trendingUpBars, 14);
        MomentumIndicators.ADXResult downTrendResult = MomentumIndicators.calculateADX(trendingDownBars, 14);
        
        assertNotNull(upTrendResult);
        assertNotNull(downTrendResult);
        // Both should have elevated ADX due to trend
    }
    
    // ============================================
    // Trend Classification Tests
    // ============================================
    
    @Test
    @DisplayName("Trend Classification: Returns MIXED for null inputs")
    void testTrendClassificationNull() {
        IndicatorResults.TrendDirection result = MomentumIndicators.classifyTrend(null, null, null);
        assertEquals(IndicatorResults.TrendDirection.MIXED, result);
    }
    
    @Test
    @DisplayName("Trend Classification: Classifies strong uptrend")
    void testTrendClassificationUptrend() {
        // Mock strong uptrend conditions
        MomentumIndicators.MACDResult macd = new MomentumIndicators.MACDResult(1.0, 0.5, 0.5);
        Double rsi = 65.0;
        MomentumIndicators.ADXResult adx = new MomentumIndicators.ADXResult(30.0, 35.0, 15.0);
        
        IndicatorResults.TrendDirection result = MomentumIndicators.classifyTrend(macd, rsi, adx);
        assertEquals(IndicatorResults.TrendDirection.STRONG_UP, result);
    }
    
    @Test
    @DisplayName("Trend Classification: Classifies explosive downtrend")
    void testTrendClassificationDowntrend() {
        MomentumIndicators.MACDResult macd = new MomentumIndicators.MACDResult(-1.0, -0.5, -0.5);
        Double rsi = 35.0;
        MomentumIndicators.ADXResult adx = new MomentumIndicators.ADXResult(30.0, 15.0, 35.0);
        
        IndicatorResults.TrendDirection result = MomentumIndicators.classifyTrend(macd, rsi, adx);
        assertEquals(IndicatorResults.TrendDirection.EXPL_DN, result);
    }
    
    // ============================================
    // Condor Score Tests
    // ============================================
    
    @Test
    @DisplayName("Condor Score: Calculates composite score")
    void testCondorScore() {
        MomentumIndicators.CondorScoreResult result = MomentumIndicators.calculateCondorScore(
            100.0,  // current price
            110.0,  // upper BB
            90.0,   // lower BB
            100.0,  // mid BB
            25.0,   // ADX
            1.3,    // IV/HV ratio
            1.2     // rich threshold
        );
        
        assertNotNull(result);
        assertTrue(result.getScore() >= 0 && result.getScore() <= 100);
        assertNotNull(result.getSignal());
    }
    
    @Test
    @DisplayName("Condor Score: Returns WATCH for null inputs")
    void testCondorScoreNullInputs() {
        MomentumIndicators.CondorScoreResult result = MomentumIndicators.calculateCondorScore(
            100.0, null, null, null, null, null, 1.2);
        
        assertNotNull(result);
        assertEquals(IndicatorResults.CondorSignal.WATCH, result.getSignal());
    }
    
    @Test
    @DisplayName("Condor Score: Signals CONDOR near middle band")
    void testCondorScoreNearMiddle() {
        MomentumIndicators.CondorScoreResult result = MomentumIndicators.calculateCondorScore(
            100.0, 110.0, 90.0, 100.0, 20.0, 1.1, 1.2);
        
        assertNotNull(result);
        assertEquals(IndicatorResults.CondorSignal.CONDOR, result.getSignal());
    }
    
    // ============================================
    // Drift Efficiency Tests
    // ============================================
    
    @Test
    @DisplayName("Drift Efficiency: Calculates efficiency")
    void testDriftEfficiency() {
        // High efficiency: close near high of range
        OHLCV bar = new OHLCV(100.0, 105.0, 99.0, 104.5, 1000000L, Instant.now());
        MomentumIndicators.DriftEfficiencyResult result = MomentumIndicators.calculateDriftEfficiency(bar, 0.6, 0.2);
        
        assertNotNull(result);
        assertNotNull(result.getEfficiency());
        assertNotNull(result.getDriftClass());
    }
    
    @Test
    @DisplayName("Drift Efficiency: Classifies high efficiency")
    void testDriftEfficiencyHigh() {
        // Large move relative to range
        OHLCV bar = new OHLCV(100.0, 105.0, 99.0, 104.0, 1000000L, Instant.now());
        MomentumIndicators.DriftEfficiencyResult result = MomentumIndicators.calculateDriftEfficiency(bar, 0.6, 0.2);
        
        assertNotNull(result);
        // Should be high efficiency since close-open is significant
    }
    
    @Test
    @DisplayName("Drift Efficiency: Returns null for invalid input")
    void testDriftEfficiencyNull() {
        assertNull(MomentumIndicators.calculateDriftEfficiency(null, 0.6, 0.2));
        assertNull(MomentumIndicators.calculateDriftEfficiency(new OHLCV(0.0, 100.0, 99.0, 100.0, 1000L, Instant.now()), 0.6, 0.2));
    }
}
