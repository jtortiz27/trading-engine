package com.trading.model.indicators;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;
import com.trading.model.indicators.engine.TechnicalIndicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Technical Indicators (ATR, VWAP, Bollinger Bands).
 */
class TechnicalIndicatorsTest {

    private List<OHLCV> sampleBars;
    private List<OHLCV> highVolBars;
    
    @BeforeEach
    void setUp() {
        sampleBars = generateSampleBars(30);
        highVolBars = generateHighVolatilityBars(30);
    }
    
    private List<OHLCV> generateSampleBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            double close = basePrice + Math.sin(i * 0.3) * 2;
            double high = close + 1.5;
            double low = close - 1.5;
            double open = close + (Math.random() - 0.5);
            long volume = 1000000 + (long)(Math.random() * 500000);
            
            bars.add(new OHLCV(open, high, low, close, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    private List<OHLCV> generateHighVolatilityBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            double close = basePrice + (Math.random() - 0.5) * 10;
            double high = close + 3.0;
            double low = close - 3.0;
            double open = close + (Math.random() - 0.5) * 2;
            long volume = 1000000L;
            
            bars.add(new OHLCV(open, high, low, close, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    private List<OHLCV> generateIntradayBars(int count) {
        List<OHLCV> bars = new ArrayList<>();
        double price = 100.0;
        
        for (int i = count - 1; i >= 0; i--) {
            price += (Math.random() - 0.48) * 0.5; // Slight upward drift
            double high = price + 0.3;
            double low = price - 0.3;
            double open = price - (Math.random() - 0.5) * 0.2;
            long volume = 100000 + i * 1000; // Increasing volume
            
            bars.add(new OHLCV(open, high, low, price, volume, 
                Instant.now().minusSeconds(i * 60)));
        }
        return bars;
    }
    
    // ============================================
    // ATR Tests
    // ============================================
    
    @Test
    @DisplayName("ATR: Calculates average true range")
    void testATR() {
        Double result = TechnicalIndicators.calculateATR(sampleBars, 14);
        assertNotNull(result);
        assertTrue(result > 0, "ATR should be positive");
    }
    
    @Test
    @DisplayName("ATR14: Calculates with default period")
    void testATR14() {
        Double result = TechnicalIndicators.calculateATR14(sampleBars);
        assertNotNull(result);
        assertTrue(result > 0);
    }
    
    @Test
    @DisplayName("ATR: Higher volatility gives higher ATR")
    void testATRVolatility() {
        Double normalATR = TechnicalIndicators.calculateATR(sampleBars, 14);
        Double highVolATR = TechnicalIndicators.calculateATR(highVolBars, 14);
        
        assertNotNull(normalATR);
        assertNotNull(highVolATR);
        assertTrue(highVolATR > normalATR, "High volatility should have higher ATR");
    }
    
    @Test
    @DisplayName("ATR: Returns null for insufficient data")
    void testATRInsufficientData() {
        List<OHLCV> smallBars = sampleBars.subList(0, 5);
        assertNull(TechnicalIndicators.calculateATR(smallBars, 14));
    }
    
    // ============================================
    // VWAP Tests
    // ============================================
    
    @Test
    @DisplayName("VWAP: Calculates VWAP with bands")
    void testVWAP() {
        List<OHLCV> intraday = generateIntradayBars(30);
        IndicatorResults.VWAPResult result = TechnicalIndicators.calculateVWAP(intraday, 1.5);
        
        assertNotNull(result);
        assertNotNull(result.getVwap());
        assertNotNull(result.getUpperBand());
        assertNotNull(result.getLowerBand());
    }
    
    @Test
    @DisplayName("VWAP: Upper band above VWAP")
    void testVWAPBands() {
        List<OHLCV> intraday = generateIntradayBars(30);
        IndicatorResults.VWAPResult result = TechnicalIndicators.calculateVWAP(intraday, 1.5);
        
        assertNotNull(result);
        assertTrue(result.getUpperBand() > result.getVwap(), "Upper band should be above VWAP");
        assertTrue(result.getLowerBand() < result.getVwap(), "Lower band should be below VWAP");
    }
    
    @Test
    @DisplayName("VWAP: Uses default 1.5 deviations")
    void testVWAPDefault() {
        List<OHLCV> intraday = generateIntradayBars(30);
        IndicatorResults.VWAPResult result = TechnicalIndicators.calculateVWAP(intraday);
        
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("VWAP: Returns null for empty bars")
    void testVWAPEmpty() {
        assertNull(TechnicalIndicators.calculateVWAP(new ArrayList<>(), 1.5));
    }
    
    @Test
    @DisplayName("VWAP: Returns null for zero volume")
    void testVWAPZeroVolume() {
        List<OHLCV> zeroVolBars = new ArrayList<>();
        zeroVolBars.add(new OHLCV(100.0, 101.0, 99.0, 100.0, 0L, Instant.now()));
        assertNull(TechnicalIndicators.calculateVWAP(zeroVolBars, 1.5));
    }
    
    // ============================================
    // Bollinger Bands Tests
    // ============================================
    
    @Test
    @DisplayName("Bollinger Bands: Calculates bands")
    void testBollingerBands() {
        IndicatorResults.BollingerBandResult result = TechnicalIndicators.calculateBollingerBands(sampleBars, 20, 2.0);
        
        assertNotNull(result);
        assertNotNull(result.getUpper());
        assertNotNull(result.getMiddle());
        assertNotNull(result.getLower());
    }
    
    @Test
    @DisplayName("Bollinger Bands: Upper > Middle > Lower")
    void testBollingerBandOrder() {
        IndicatorResults.BollingerBandResult result = TechnicalIndicators.calculateBollingerBands(sampleBars, 20, 2.0);
        
        assertNotNull(result);
        assertTrue(result.getUpper() > result.getMiddle(), "Upper should be above middle");
        assertTrue(result.getMiddle() > result.getLower(), "Middle should be above lower");
    }
    
    @Test
    @DisplayName("Bollinger Bands: Middle equals SMA")
    void testBollingerMiddle() {
        // Calculate SMA manually
        double sum = 0;
        for (int i = 0; i < 20 && i < sampleBars.size(); i++) {
            sum += sampleBars.get(i).getClose();
        }
        double expectedSMA = sum / 20;
        
        IndicatorResults.BollingerBandResult result = TechnicalIndicators.calculateBollingerBands(sampleBars, 20, 2.0);
        
        assertNotNull(result);
        assertEquals(expectedSMA, result.getMiddle(), 0.001, "Middle band should equal 20-period SMA");
    }
    
    @Test
    @DisplayName("Bollinger Bands: Uses defaults (20, 2.0)")
    void testBollingerDefaults() {
        IndicatorResults.BollingerBandResult result = TechnicalIndicators.calculateBollingerBands(sampleBars);
        
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Bollinger Bands: Returns null for insufficient data")
    void testBollingerInsufficientData() {
        List<OHLCV> smallBars = sampleBars.subList(0, 10);
        assertNull(TechnicalIndicators.calculateBollingerBands(smallBars, 20, 2.0));
    }
    
    @Test
    @DisplayName("Bollinger Bands: Higher volatility gives wider bands")
    void testBollingerWidth() {
        IndicatorResults.BollingerBandResult normalResult = TechnicalIndicators.calculateBollingerBands(sampleBars, 20, 2.0);
        IndicatorResults.BollingerBandResult highVolResult = TechnicalIndicators.calculateBollingerBands(highVolBars, 20, 2.0);
        
        assertNotNull(normalResult);
        assertNotNull(highVolResult);
        
        double normalWidth = normalResult.getUpper() - normalResult.getLower();
        double highVolWidth = highVolResult.getUpper() - highVolResult.getLower();
        
        assertTrue(highVolWidth > normalWidth, "High volatility should have wider bands");
    }
    
    // ============================================
    // Integration Tests
    // ============================================
    
    @Test
    @DisplayName("Integration: All indicators can be calculated from same data")
    void testAllIndicators() {
        List<OHLCV> bars = generateSampleBars(50);
        
        Double atr = TechnicalIndicators.calculateATR14(bars);
        IndicatorResults.VWAPResult vwap = TechnicalIndicators.calculateVWAP(bars.subList(0, Math.min(30, bars.size())));
        IndicatorResults.BollingerBandResult bb = TechnicalIndicators.calculateBollingerBands(bars);
        
        assertNotNull(atr);
        assertNotNull(vwap);
        assertNotNull(bb);
        
        // Verify reasonable relationships
        assertTrue(atr > 0);
        assertTrue(vwap.getUpperBand() > vwap.getVwap());
        assertTrue(bb.getUpper() > bb.getMiddle());
    }
}
