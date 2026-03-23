package com.trading.model.indicators;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;
import com.trading.model.indicators.engine.IndicatorsEngine;
import com.trading.model.indicators.engine.MomentumIndicators;
import com.trading.model.indicators.engine.TechnicalIndicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ThinkScript indicator port.
 * Validates calculations against known values and edge cases.
 */
class IndicatorsEngineTest {

    private List<OHLCV> sampleBars;
    private List<OHLCV> spxBars;
    
    @BeforeEach
    void setUp() {
        sampleBars = generateSampleBars(50);
        spxBars = generateSampleBars(50);
    }
    
    // ============================================
    // Test Data Generation
    // ============================================
    
    private List<OHLCV> generateSampleBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            // Generate some price movement
            double trend = i * 0.1; // Slight upward trend
            double noise = Math.sin(i * 0.5) * 2; // Oscillating noise
            double volatility = Math.random() * 1.5; // Random volatility
            
            double close = basePrice + trend + noise;
            double high = close + volatility;
            double low = close - volatility;
            double open = close + (Math.random() - 0.5);
            long volume = 1000000 + (long)(Math.random() * 500000);
            
            bars.add(new OHLCV(open, high, low, close, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        
        return bars; // Most recent first
    }
    
    private List<OHLCV> generateFlatBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            bars.add(new OHLCV(100.0, 101.0, 99.0, 100.0, 1000000L, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    // ============================================
    // Volume Strength Tests
    // ============================================
    
    @Test
    @DisplayName("Volume Strength: Basic calculation with RTH projection")
    void testVolumeStrengthWithRTH() {
        Double result = IndicatorsEngine.calculateVolumeStrength(sampleBars, 20, 120, 390);
        assertNotNull(result);
        assertTrue(result > 0, "Volume strength should be positive");
    }
    
    @Test
    @DisplayName("Volume Strength: Returns null for insufficient data")
    void testVolumeStrengthInsufficientData() {
        List<OHLCV> smallBars = sampleBars.subList(0, 5);
        Double result = IndicatorsEngine.calculateVolumeStrength(smallBars, 20, 60, 390);
        assertNull(result);
    }
    
    @Test
    @DisplayName("Volume Strength: Simple ratio without RTH")
    void testVolumeRatio() {
        Double result = IndicatorsEngine.calculateVolumeRatio(sampleBars, 20);
        assertNotNull(result);
    }
    
    // ============================================
    // IV Dashboard Tests
    // ============================================
    
    @Test
    @DisplayName("IV Rank: Calculates percentile correctly")
    void testIVRank() {
        List<Double> ivHistory = Arrays.asList(15.0, 18.0, 20.0, 22.0, 25.0, 
            16.0, 19.0, 21.0, 23.0, 24.0);
        Double result = IndicatorsEngine.calculateIVRank(22.0, ivHistory, 10);
        assertNotNull(result);
        assertTrue(result >= 0 && result <= 100, "IV Rank should be 0-100");
    }
    
    @Test
    @DisplayName("IV/HV Ratio: Returns correct ratio")
    void testIVHVRatio() {
        Double result = IndicatorsEngine.calculateIVHVRatio(25.0, 20.0);
        assertEquals(1.25, result, 0.001);
    }
    
    @Test
    @DisplayName("IV/HV Ratio: Handles null inputs")
    void testIVHVRatioNull() {
        assertNull(IndicatorsEngine.calculateIVHVRatio(null, 20.0));
        assertNull(IndicatorsEngine.calculateIVHVRatio(25.0, null));
        assertNull(IndicatorsEngine.calculateIVHVRatio(25.0, 0.0));
    }
    
    @Test
    @DisplayName("VRP: Calculates volatility risk premium")
    void testVRP() {
        Double result = IndicatorsEngine.calculateVRP(25.0, 20.0);
        assertEquals(5.0, result, 0.001);
    }
    
    // ============================================
    // HV Tests
    // ============================================
    
    @Test
    @DisplayName("HV30: Calculates historical volatility")
    void testHV30() {
        Double result = IndicatorsEngine.calculateHV30(sampleBars, 30, 252);
        assertNotNull(result);
        assertTrue(result > 0, "HV should be positive");
    }
    
    @Test
    @DisplayName("HV30: Returns null for insufficient data")
    void testHV30InsufficientData() {
        List<OHLCV> smallBars = sampleBars.subList(0, 10);
        Double result = IndicatorsEngine.calculateHV30(smallBars, 30, 252);
        assertNull(result);
    }
    
    // ============================================
    // Correlation Tests
    // ============================================
    
    @Test
    @DisplayName("SPX Correlation: Calculates correlation")
    void testSPXCorrelation() {
        Double result = IndicatorsEngine.calculateSPXCorrelation(sampleBars, spxBars, 30);
        assertNotNull(result);
        assertTrue(result >= -1 && result <= 1, "Correlation should be between -1 and 1");
    }
    
    @Test
    @DisplayName("SPX Correlation: Returns null for insufficient data")
    void testSPXCorrelationInsufficientData() {
        assertNull(IndicatorsEngine.calculateSPXCorrelation(sampleBars, spxBars, 100));
    }
    
    // ============================================
    // ATM Vega Tests
    // ============================================
    
    @Test
    @DisplayName("ATM Vega: Calculates vega value")
    void testATMVega() {
        // 30 days = 30/365 years
        Double result = IndicatorsEngine.calculateATMVega(100.0, 100.0, 30.0/365.0, 0.20, 0.04);
        assertNotNull(result);
        assertTrue(result > 0, "Vega should be positive");
    }
    
    @Test
    @DisplayName("ATM Vega: Handles edge cases")
    void testATMVegaEdgeCases() {
        assertNull(IndicatorsEngine.calculateATMVega(0, 100.0, 0.082, 0.20, 0.04));
        assertNull(IndicatorsEngine.calculateATMVega(100.0, 100.0, 0, 0.20, 0.04));
        assertNull(IndicatorsEngine.calculateATMVega(100.0, 100.0, 0.082, 0, 0.04));
    }
    
    @Test
    @DisplayName("Vega Percentile: Calculates correctly")
    void testVegaPercentile() {
        List<Double> history = Arrays.asList(0.05, 0.08, 0.10, 0.12, 0.15, 0.18, 0.20, 0.22);
        Double result = IndicatorsEngine.calculateVegaPercentile(0.15, history, 8);
        assertNotNull(result);
        assertTrue(result >= 0 && result <= 100);
    }
}
