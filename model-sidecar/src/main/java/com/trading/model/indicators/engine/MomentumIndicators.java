package com.trading.model.indicators.engine;

import com.trading.model.indicators.domain.IndicatorResults;
import com.trading.model.indicators.domain.OHLCV;

import java.util.ArrayList;
import java.util.List;

/**
 * Momentum and Trend Indicators.
 * 
 * ThinkScript → Java Mapping for indicators:
 * - MACD: def macd = MACD(fast, slow, signal) → Custom EMA-based calculation
 * - RSI: def rsi = RSI(length) → Wilder RSI implementation
 * - ADX: def adx = ADX(length) → Wilder ADX with +DI/-DI
 * - Trend Arrows: Based on composite of MACD, RSI, ADX conditions
 */
public class MomentumIndicators {

    // ============================================
    // 6. Momentum/Trend Arrows - MACD, RSI, ADX-based trend taxonomy
    // ============================================
    
    /**
     * Calculates MACD (Moving Average Convergence Divergence).
     * 
     * ThinkScript:
     * def fastEMA = ExpAverage(close, fastLength);
     * def slowEMA = ExpAverage(close, slowLength);
     * def macdLine = fastEMA - slowEMA;
     * def signalLine = ExpAverage(macdLine, signalLength);
     * def histogram = macdLine - signalLine;
     * 
     * @param bars OHLCV bars (most recent first)
     * @param fastLength Fast EMA period (default: 12)
     * @param slowLength Slow EMA period (default: 26)
     * @param signalLength Signal line period (default: 9)
     * @return MACDResult with macdLine, signalLine, and histogram
     */
    public static MACDResult calculateMACD(List<OHLCV> bars, int fastLength, int slowLength, int signalLength) {
        if (bars == null || bars.size() < slowLength + signalLength) {
            return null;
        }
        
        double[] closes = bars.stream().mapToDouble(OHLCV::getClose).toArray();
        
        double fastEMA = calculateEMA(closes, fastLength);
        double slowEMA = calculateEMA(closes, slowLength);
        double macdLine = fastEMA - slowEMA;
        
        // Calculate signal line (EMA of MACD)
        // We need historical MACD values
        double[] macdHistory = new double[slowLength];
        for (int i = 0; i < slowLength && i + slowLength < closes.length; i++) {
            double fast = calculateEMAAtIndex(closes, fastLength, i);
            double slow = calculateEMAAtIndex(closes, slowLength, i);
            macdHistory[i] = fast - slow;
        }
        
        double signalLine = calculateEMA(macdHistory, signalLength);
        double histogram = macdLine - signalLine;
        
        return new MACDResult(macdLine, signalLine, histogram);
    }
    
    /**
     * Calculates RSI (Relative Strength Index) using Wilder's smoothing.
     * 
     * ThinkScript:
     * def change = close - close[1];
     * def gain = if change > 0 then change else 0;
     * def loss = if change < 0 then -change else 0;
     * def avgGain = Average(gain, length);
     * def avgLoss = Average(loss, length);
     * def rs = avgGain / avgLoss;
     * def rsi = 100 - (100 / (1 + rs));
     * 
     * @param bars OHLCV bars (most recent first)
     * @param length RSI period (default: 14)
     * @return RSI value (0-100)
     */
    public static Double calculateRSI(List<OHLCV> bars, int length) {
        if (bars == null || bars.size() < length + 1) {
            return null;
        }
        
        double[] gains = new double[length];
        double[] losses = new double[length];
        
        for (int i = 0; i < length; i++) {
            double change = bars.get(i).getClose() - bars.get(i + 1).getClose();
            if (change > 0) {
                gains[i] = change;
                losses[i] = 0;
            } else {
                gains[i] = 0;
                losses[i] = -change;
            }
        }
        
        double avgGain = wilderSmooth(gains, length);
        double avgLoss = wilderSmooth(losses, length);
        
        if (avgLoss == 0) {
            return 100.0;
        }
        
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
    
    /**
     * Calculates ADX (Average Directional Index) with +DI and -DI.
     * 
     * ThinkScript:
     * def tr = TrueRange(high, close, low);
     * def atr = Average(tr, length);
     * def plusDM = if high - high[1] > low[1] - low then Max(high - high[1], 0) else 0;
     * def minusDM = if low[1] - low > high - high[1] then Max(low[1] - low, 0) else 0;
     * def plusDI = 100 * Average(plusDM, length) / atr;
     * def minusDI = 100 * Average(minusDM, length) / atr;
     * def dx = 100 * Abs(plusDI - minusDI) / (plusDI + minusDI);
     * def adx = Average(dx, length);
     * 
     * @param bars OHLCV bars (most recent first)
     * @param length ADX period (default: 14)
     * @return ADXResult with adx, plusDI, and minusDI
     */
    public static ADXResult calculateADX(List<OHLCV> bars, int length) {
        if (bars == null || bars.size() < length + 1) {
            return null;
        }
        
        double[] tr = new double[length];
        double[] plusDM = new double[length];
        double[] minusDM = new double[length];
        
        for (int i = 0; i < length; i++) {
            double high = bars.get(i).getHigh();
            double low = bars.get(i).getLow();
            double close = bars.get(i).getClose();
            double prevHigh = bars.get(i + 1).getHigh();
            double prevLow = bars.get(i + 1).getLow();
            double prevClose = bars.get(i + 1).getClose();
            
            // True Range
            tr[i] = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            
            // Directional Movement
            double upMove = high - prevHigh;
            double downMove = prevLow - low;
            
            if (upMove > downMove && upMove > 0) {
                plusDM[i] = upMove;
            } else {
                plusDM[i] = 0;
            }
            
            if (downMove > upMove && downMove > 0) {
                minusDM[i] = downMove;
            } else {
                minusDM[i] = 0;
            }
        }
        
        double atr = wilderSmooth(tr, length);
        double smoothedPlusDM = wilderSmooth(plusDM, length);
        double smoothedMinusDM = wilderSmooth(minusDM, length);
        
        double plusDI = (atr == 0) ? 0 : 100.0 * smoothedPlusDM / atr;
        double minusDI = (atr == 0) ? 0 : 100.0 * smoothedMinusDM / atr;
        
        double diDiff = Math.abs(plusDI - minusDI);
        double diSum = plusDI + minusDI;
        
        double dx = (diSum == 0) ? 0 : 100.0 * diDiff / diSum;
        
        // ADX is smoothed DX
        double adx = wilderSmooth(new double[]{dx}, length);
        
        return new ADXResult(adx, plusDI, minusDI);
    }
    
    /**
     * Classifies trend direction based on MACD, RSI, and ADX.
     * 
     * ThinkScript Trend Taxonomy:
     * - STRONG_UP: MACD bullish, RSI > 50, ADX > 25
     * - EXPL_DN: MACD bearish, RSI < 50, high momentum
     * - FADE: Pullback in uptrend
     * - PULL_REC: Pullback recovery
     * - FLAT: No clear trend
     * - MIXED: Conflicting signals
     * 
     * @param macd MACD result
     * @param rsi RSI value
     * @param adx ADX result
     * @return TrendDirection classification
     */
    public static IndicatorResults.TrendDirection classifyTrend(
            MACDResult macd, 
            Double rsi, 
            ADXResult adx) {
        
        if (macd == null || rsi == null || adx == null) {
            return IndicatorResults.TrendDirection.MIXED;
        }
        
        boolean macdBullish = macd.getHistogram() > 0;
        boolean rsiBullish = rsi > 50;
        boolean strongTrend = adx.getAdx() > 25;
        boolean weakTrend = adx.getAdx() < 20;
        boolean plusDIDominates = adx.getPlusDI() > adx.getMinusDI();

        // Strong uptrend
        if (macdBullish && rsiBullish && strongTrend && plusDIDominates) {
            return IndicatorResults.TrendDirection.STRONG_UP;
        }
        
        // Strong downtrend / Explosive down
        if (!macdBullish && !rsiBullish && strongTrend && !plusDIDominates) {
            return IndicatorResults.TrendDirection.EXPL_DN;
        }
        
        // Fade - pullback in strong trend
        if (strongTrend && !macdBullish && rsiBullish) {
            return IndicatorResults.TrendDirection.FADE;
        }
        
        // Pullback recovery
        if (strongTrend && macdBullish && !rsiBullish) {
            return IndicatorResults.TrendDirection.PULL_REC;
        }
        
        // Flat - weak trend
        if (weakTrend) {
            return IndicatorResults.TrendDirection.FLAT;
        }
        
        // Mixed signals
        return IndicatorResults.TrendDirection.MIXED;
    }
    
    // ============================================
    // 7. CondorScore - Proximity + ADX + IV/HV composite
    // ============================================
    
    /**
     * Calculates Condor Score for options strategy selection.
     * 
     * ThinkScript Composite:
     * def proximityScore = proximity to support/resistance
     * def adxScore = normalize(ADX)  
     * def ivScore = normalize(IV/HV ratio)
     * def condorScore = weightedAverage(proximityScore, adxScore, ivScore)
     * 
     * @param currentPrice Current stock price
     * @param bollingerUpper Upper Bollinger Band
     * @param bollingerLower Lower Bollinger Band
     * @param bollingerMid Middle Bollinger Band (SMA)
     * @param adx ADX value
     * @param ivHvRatio IV/HV ratio
     * @param ivhvRich Threshold for rich IV (default: 1.2)
     * @return CondorScoreResult with score and signal
     */
    public static CondorScoreResult calculateCondorScore(
            double currentPrice,
            Double bollingerUpper,
            Double bollingerLower,
            Double bollingerMid,
            Double adx,
            Double ivHvRatio,
            double ivhvRich) {
        
        if (bollingerUpper == null || bollingerLower == null || bollingerMid == null || 
            adx == null || ivHvRatio == null) {
            return new CondorScoreResult(0.0, IndicatorResults.CondorSignal.WATCH);
        }
        
        // Proximity score (0-100): how close to middle of BB
        double bbRange = bollingerUpper - bollingerLower;
        double proximityToMid = Math.abs(currentPrice - bollingerMid);
        double proximityScore = (bbRange == 0) ? 100 : 100 * (1 - proximityToMid / bbRange);
        
        // ADX score (0-100): higher ADX = better for range-bound strategies
        double adxScore = Math.min(100, adx * 4); // Scale ADX (25+ = 100)
        
        // IV/HV score (0-100): elevated IV = better premium
        double ivScore = (ivHvRatio > ivhvRich) ? 100 : (ivHvRatio > 1.0) ? 75 : 50;
        
        // Composite score (weighted)
        double condorScore = (proximityScore * 0.4 + adxScore * 0.3 + ivScore * 0.3);
        
        // Signal classification
        IndicatorResults.CondorSignal signal = classifyCondorSignal(
            currentPrice, bollingerUpper, bollingerLower, bollingerMid, 
            adx, ivHvRatio, ivhvRich);
        
        return new CondorScoreResult(condorScore, signal);
    }
    
    /**
     * Classifies Condor signal based on price position and market conditions.
     * 
     * ThinkScript:
     * def signal = if price > upper * 0.95 then BEAR_CREDIT
     * else if price < lower * 1.05 then BULL_CREDIT
     * else if price > mid && adx > 25 then BEAR_DEBIT
     * else if price < mid && adx > 25 then BULL_DEBIT
     * else if proximity < 5 then CONDOR
     * else WATCH;
     * 
     * @return CondorSignal classification
     */
    private static IndicatorResults.CondorSignal classifyCondorSignal(
            double price,
            double upper,
            double lower,
            double mid,
            double adx,
            double ivHvRatio,
            double ivhvRich) {
        
        double proximityToUpper = Math.abs(price - upper) / upper;
        double proximityToLower = Math.abs(price - lower) / lower;
        double proximityToMid = Math.abs(price - mid) / mid;
        
        boolean nearUpper = price > upper * 0.95;
        boolean nearLower = price < lower * 1.05;
        boolean nearMid = proximityToMid < 0.02; // Within 2% of mid
        boolean strongTrend = adx > 25;
        boolean ivRich = ivHvRatio > ivhvRich;
        
        if (nearUpper) {
            return ivRich ? IndicatorResults.CondorSignal.BEAR_CREDIT : IndicatorResults.CondorSignal.BEAR_DEBIT;
        }
        
        if (nearLower) {
            return ivRich ? IndicatorResults.CondorSignal.BULL_CREDIT : IndicatorResults.CondorSignal.BULL_DEBIT;
        }
        
        if (nearMid) {
            return IndicatorResults.CondorSignal.CONDOR;
        }
        
        return IndicatorResults.CondorSignal.WATCH;
    }

    // ============================================
    // 8. Drift Efficiency - (close-open)/open vs range
    // ============================================
    
    /**
     * Calculates Drift Efficiency.
     * Measures how efficiently price moved from open to close relative to total range.
     * 
     * ThinkScript:
     * def drift = (close - open) / open;
     * def range = high - low;
     * def driftEfficiency = Abs(drift) / (range / close);
     * 
     * High efficiency: large close-open move relative to intraday range
     * Low efficiency: small close-open move but large intraday range
     * 
     * @param bar Current OHLCV bar
     * @param driftEffHi High efficiency threshold (default: 0.6)
     * @param driftEffLo Low efficiency threshold (default: 0.2)
     * @return DriftEfficiencyResult with value and classification
     */
    public static DriftEfficiencyResult calculateDriftEfficiency(
            OHLCV bar,
            double driftEffHi,
            double driftEffLo) {
        
        if (bar == null || bar.getOpen() == 0 || bar.getClose() == 0) {
            return null;
        }
        
        double open = bar.getOpen();
        double close = bar.getClose();
        double high = bar.getHigh();
        double low = bar.getLow();
        
        double drift = (close - open) / open;
        double range = high - low;
        
        if (range == 0) {
            return null;
        }
        
        // Drift efficiency: how much of the range was "used" by the close
        double driftEfficiency = Math.abs(drift) / (range / close);
        
        // Classification
        IndicatorResults.DriftEfficiencyClass driftClass;
        if (driftEfficiency >= driftEffHi) {
            driftClass = IndicatorResults.DriftEfficiencyClass.HIGH_EFFICIENCY;
        } else if (driftEfficiency >= driftEffLo) {
            driftClass = IndicatorResults.DriftEfficiencyClass.MODERATE_EFFICIENCY;
        } else {
            driftClass = IndicatorResults.DriftEfficiencyClass.LOW_EFFICIENCY;
        }
        
        return new DriftEfficiencyResult(driftEfficiency, driftClass);
    }

    // ============================================
    // Result classes
    // ============================================
    
    public static class MACDResult {
        private final double macdLine;
        private final double signalLine;
        private final double histogram;
        
        public MACDResult(double macdLine, double signalLine, double histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }
        
        public double getMacdLine() { return macdLine; }
        public double getSignalLine() { return signalLine; }
        public double getHistogram() { return histogram; }
    }
    
    public static class ADXResult {
        private final double adx;
        private final double plusDI;
        private final double minusDI;
        
        public ADXResult(double adx, double plusDI, double minusDI) {
            this.adx = adx;
            this.plusDI = plusDI;
            this.minusDI = minusDI;
        }
        
        public double getAdx() { return adx; }
        public double getPlusDI() { return plusDI; }
        public double getMinusDI() { return minusDI; }
    }
    
    public static class CondorScoreResult {
        private final double score;
        private final IndicatorResults.CondorSignal signal;
        
        public CondorScoreResult(double score, IndicatorResults.CondorSignal signal) {
            this.score = score;
            this.signal = signal;
        }
        
        public double getScore() { return score; }
        public IndicatorResults.CondorSignal getSignal() { return signal; }
    }
    
    public static class DriftEfficiencyResult {
        private final double efficiency;
        private final IndicatorResults.DriftEfficiencyClass driftClass;
        
        public DriftEfficiencyResult(double efficiency, IndicatorResults.DriftEfficiencyClass driftClass) {
            this.efficiency = efficiency;
            this.driftClass = driftClass;
        }
        
        public double getEfficiency() { return efficiency; }
        public IndicatorResults.DriftEfficiencyClass getDriftClass() { return driftClass; }
    }

    // ============================================
    // Helper methods
    // ============================================
    
    /**
     * Calculates Exponential Moving Average (EMA).
     * ThinkScript: ExpAverage(data, length)
     */
    private static double calculateEMA(double[] data, int length) {
        return calculateEMAAtIndex(data, length, 0);
    }
    
    /**
     * Calculates EMA at specific index.
     */
    private static double calculateEMAAtIndex(double[] data, int length, int startIndex) {
        if (data.length < length + startIndex) {
            return data[startIndex];
        }
        
        double multiplier = 2.0 / (length + 1);
        double ema = data[length - 1 + startIndex]; // Start with SMA
        
        for (int i = length - 2 + startIndex; i >= startIndex; i--) {
            ema = (data[i] - ema) * multiplier + ema;
        }
        
        return ema;
    }
    
    /**
     * Wilder's smoothing (RSI/ADX standard).
     * ThinkScript: Average with Wilder's alpha = 1/length
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
