package com.trading.model.indicators;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;
import com.trading.model.indicators.service.IndicatorsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for IndicatorsService.
 * Tests the complete indicator calculation pipeline.
 */
class IndicatorsServiceTest {

    private IndicatorsService service;
    private List<OHLCV> sampleBars;
    private List<OHLCV> spxBars;
    private List<Double> ivHistory;
    private List<Double> hvHistory;
    private List<Double> vegaHistory;
    
    @BeforeEach
    void setUp() {
        service = new IndicatorsService();
        sampleBars = generateSampleBars(300);
        spxBars = generateSampleBars(300);
        ivHistory = generateIVHistory(260);
        hvHistory = generateHVHistory(260);
        vegaHistory = generateVegaHistory(260);
    }
    
    private List<OHLCV> generateSampleBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            double trend = i * 0.05;
            double noise = Math.sin(i * 0.3) * 2;
            double volatility = 1.0 + Math.random() * 0.5;
            
            double close = basePrice + trend + noise;
            double high = close + volatility;
            double low = close - volatility;
            double open = close + (Math.random() - 0.5);
            long volume = 1000000 + (long)(Math.random() * 500000);
            
            bars.add(new OHLCV(open, high, low, close, volume, 
                Instant.now().minusSeconds(i * 86400))); // Daily bars
        }
        return bars;
    }
    
    private List<Double> generateIVHistory(int count) {
        List<Double> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            history.add(15.0 + Math.random() * 15.0); // IV between 15-30%
        }
        return history;
    }
    
    private List<Double> generateHVHistory(int count) {
        List<Double> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            history.add(12.0 + Math.random() * 10.0); // HV between 12-22%
        }
        return history;
    }
    
    private List<Double> generateVegaHistory(int count) {
        List<Double> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            history.add(0.05 + Math.random() * 0.15); // Vega between 0.05-0.20
        }
        return history;
    }
    
    // ============================================
    // Full Calculation Tests
    // ============================================
    
    @Test
    @DisplayName("Calculate All Indicators: Full pipeline")
    void testCalculateAllIndicators() {
        IndicatorResults results = service.calculateAllIndicators(
            sampleBars,
            spxBars,
            ivHistory,
            hvHistory,
            vegaHistory,
            22.0,  // current IV
            18.0,  // current HV
            195.0, // RTH minutes elapsed (midday)
            390.0  // Total RTH minutes
        );
        
        assertNotNull(results);
        
        // Verify all expected fields are populated
        assertNotNull(results.getVolumeStrength());
        assertNotNull(results.getImpliedVolatility());
        assertNotNull(results.getIvRank());
        assertNotNull(results.getIvHvRatio());
        assertNotNull(results.getVolatilityRiskPremium());
        assertNotNull(results.getSpxCorrelation());
        assertNotNull(results.getAtmVega30Day());
        assertNotNull(results.getTrendDirection());
        assertNotNull(results.getMacdLine());
        assertNotNull(results.getRsiValue());
        assertNotNull(results.getAdxValue());
        assertNotNull(results.getCondorScore());
        assertNotNull(results.getCondorSignal());
        assertNotNull(results.getDriftEfficiency());
        assertNotNull(results.getDriftClass());
        assertNotNull(results.getAtr14());
        assertNotNull(results.getVwap());
        assertNotNull(results.getBollingerBands());
    }
    
    @Test
    @DisplayName("Calculate All Indicators: Handles null inputs gracefully")
    void testCalculateAllIndicatorsNullInputs() {
        IndicatorResults results = service.calculateAllIndicators(
            sampleBars,
            null,  // no SPX bars
            null,  // no IV history
            null,  // no HV history
            null,  // no vega history
            null,  // no current IV
            null,  // no current HV
            195.0,
            390.0
        );
        
        assertNotNull(results);
        // Should still calculate indicators that don't require optional inputs
        assertNotNull(results.getVolumeStrength());
        assertNotNull(results.getTrendDirection());
        assertNotNull(results.getAtr14());
        assertNotNull(results.getVwap());
        assertNotNull(results.getBollingerBands());
        
        // These should be null due to missing inputs
        assertNull(results.getSpxCorrelation());
        assertNull(results.getIvRank());
        assertNull(results.getIvHvRatio());
    }
    
    @Test
    @DisplayName("Calculate Basic Indicators: Minimal inputs")
    void testCalculateBasicIndicators() {
        IndicatorResults results = service.calculateBasicIndicators(sampleBars);
        
        assertNotNull(results);
        assertNotNull(results.getVolumeStrength());
        assertNotNull(results.getTrendDirection());
        assertNotNull(results.getAtr14());
        assertNotNull(results.getVwap());
        assertNotNull(results.getBollingerBands());
    }
    
    @Test
    @DisplayName("Calculate All Indicators: Returns null for empty bars")
    void testCalculateAllIndicatorsEmptyBars() {
        IndicatorResults results = service.calculateAllIndicators(
            new ArrayList<>(),
            spxBars,
            ivHistory,
            hvHistory,
            vegaHistory,
            22.0,
            18.0,
            195.0,
            390.0
        );
        
        assertNull(results);
    }
    
    // ============================================
    // Individual Method Tests
    // ============================================
    
    @Test
    @DisplayName("Service: Calculate Volume Strength")
    void testServiceVolumeStrength() {
        Double result = service.calculateVolumeStrength(sampleBars, 195.0, 390.0);
        assertNotNull(result);
        assertTrue(result > 0);
    }
    
    @Test
    @DisplayName("Service: Calculate ATR14")
    void testServiceATR14() {
        Double result = service.calculateATR14(sampleBars);
        assertNotNull(result);
        assertTrue(result > 0);
    }
    
    @Test
    @DisplayName("Service: Calculate Bollinger Bands")
    void testServiceBollingerBands() {
        IndicatorResults.BollingerBandResult result = service.calculateBollingerBands(sampleBars);
        assertNotNull(result);
        assertTrue(result.getUpper() > result.getMiddle());
        assertTrue(result.getMiddle() > result.getLower());
    }
    
    @Test
    @DisplayName("Service: Calculate VWAP")
    void testServiceVWAP() {
        IndicatorResults.VWAPResult result = service.calculateVWAP(sampleBars);
        assertNotNull(result);
        assertTrue(result.getUpperBand() > result.getVwap());
        assertTrue(result.getLowerBand() < result.getVwap());
    }
    
    @Test
    @DisplayName("Service: Calculate MACD")
    void testServiceMACD() {
        var result = service.calculateMACD(sampleBars);
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Service: Calculate RSI")
    void testServiceRSI() {
        Double result = service.calculateRSI(sampleBars);
        assertNotNull(result);
        assertTrue(result >= 0 && result <= 100);
    }
    
    @Test
    @DisplayName("Service: Calculate ADX")
    void testServiceADX() {
        var result = service.calculateADX(sampleBars);
        assertNotNull(result);
        assertTrue(result.getAdx() >= 0);
    }
    
    // ============================================
    // Edge Case Tests
    // ============================================
    
    @Test
    @DisplayName("Edge Case: Very small dataset")
    void testSmallDataset() {
        List<OHLCV> smallBars = sampleBars.subList(0, 30);
        IndicatorResults results = service.calculateBasicIndicators(smallBars);
        
        // Some indicators may be null due to insufficient data
        assertNotNull(results);
    }
    
    @Test
    @DisplayName("Edge Case: Flat prices")
    void testFlatPrices() {
        List<OHLCV> flatBars = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            flatBars.add(new OHLCV(100.0, 101.0, 99.0, 100.0, 1000000L, 
                Instant.now().minusSeconds(i * 60)));
        }
        
        IndicatorResults results = service.calculateBasicIndicators(flatBars);
        assertNotNull(results);
    }
}
