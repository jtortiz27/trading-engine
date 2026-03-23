package com.trading.model.indicators.engine;

import com.trading.model.indicators.domain.OHLCV;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IndicatorsEngine - Pure Java implementation of ThinkScript indicators.
 * 
 * ThinkScript → Java Mapping:
 * - ThinkScript series operations → Java stream/list operations
 * - ThinkScript built-ins (Average, StDev, etc) → Apache Commons Math
 * - ThinkScript wilder's smoothing → Custom Wilder smoothing implementation
 * - ThinkScript compoundValue → Loop accumulation
 * 
 * All calculations are pure functions with no side effects.
 */
public class IndicatorsEngine {

    // ============================================
    // 1. VolumeStrength - VR (volume ratio) with RTH time projection
    // ============================================
    
    /**
     * Calculates Volume Strength Ratio with RTH (Regular Trading Hours) projection.
     * 
     * ThinkScript equivalent:
     * def vr = volume / Average(volume, volLookBack)
     * def rthElapsed = (currentTime - rthStart) / (rthEnd - rthStart)
     * def rthProj = vr / rthElapsed
     * 
     * @param bars List of OHLCV bars (most recent first)
     * @param volLookBack Period for average volume calculation (default: 20)
     * @param rthMinutesElapsed Minutes elapsed since RTH start
     * @param rthTotalMinutes Total RTH minutes (e.g., 390 for 9:30-16:00)
     * @return Volume strength ratio
     */
    public static Double calculateVolumeStrength(
            List<OHLCV> bars, 
            int volLookBack,
            double rthMinutesElapsed,
            double rthTotalMinutes) {
        
        if (bars == null || bars.size() < volLookBack) {
            return null;
        }
        
        double currentVolume = bars.get(0).getVolume();
        double avgVolume = calculateSMA(bars.stream()
                .mapToDouble(OHLCV::getVolume)
                .toArray(), volLookBack);
        
        if (avgVolume == 0 || rthMinutesElapsed == 0) {
            return null;
        }
        
        double vr = currentVolume / avgVolume;
        double rthProgress = rthMinutesElapsed / rthTotalMinutes;
        
        // RTH projection: estimate full-day volume
        return vr / rthProgress;
    }
    
    /**
     * Simple volume ratio without RTH projection.
     * ThinkScript: def vr = volume / Average(volume, volLookBack)
     */
    public static Double calculateVolumeRatio(List<OHLCV> bars, int volLookBack) {
        if (bars == null || bars.size() < volLookBack) {
            return null;
        }
        
        double currentVolume = bars.get(0).getVolume();
        double avgVolume = calculateSMA(bars.stream()
                .mapToDouble(OHLCV::getVolume)
                .toArray(), volLookBack);
        
        return avgVolume == 0 ? null : currentVolume / avgVolume;
    }

    // ============================================
    // 2. IV Dashboard - IV, IV-Rank, IV/HV ratio, VRP
    // ============================================
    
    /**
     * Calculates IV Rank as percentile over lookback period.
     * ThinkScript equivalent: Rank calculation using percentile
     * 
     * @param currentIv Current implied volatility
     * @param ivHistory Historical IV values (most recent first)
     * @param rankLen Lookback period for ranking (default: 252)
     * @return IV Rank (0-100)
     */
    public static Double calculateIVRank(double currentIv, List<Double> ivHistory, int rankLen) {
        if (ivHistory == null || ivHistory.size() < rankLen) {
            return null;
        }
        
        List<Double> relevantHistory = ivHistory.subList(0, Math.min(rankLen, ivHistory.size()));
        double minIv = relevantHistory.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxIv = relevantHistory.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        if (maxIv == minIv) {
            return 50.0; // Equal distribution
        }
        
        return 100.0 * (currentIv - minIv) / (maxIv - minIv);
    }
    
    /**
     * Calculates IV/HV ratio.
     * ThinkScript: def ivHvRatio = iv / hv
     * 
     * @param iv Current implied volatility
     * @param hv Historical volatility
     * @return IV/HV ratio
     */
    public static Double calculateIVHVRatio(Double iv, Double hv) {
        if (iv == null || hv == null || hv == 0) {
            return null;
        }
        return iv / hv;
    }
    
    /**
     * Calculates Volatility Risk Premium (VRP).
     * ThinkScript: def vrp = iv - hv
     * 
     * @param iv Implied volatility
     * @param hv Historical volatility
     * @return VRP value
     */
    public static Double calculateVRP(Double iv, Double hv) {
        if (iv == null || hv == null) {
            return null;
        }
        return iv - hv;
    }

    // ============================================
    // 3. HV30 Percentile - Historical vol percentile
    // ============================================
    
    /**
     * Calculates Historical Volatility (HV) using standard deviation of log returns.
     * ThinkScript equivalent: Log returns stdev annualized
     * 
     * @param bars Price bars (most recent first)
     * @param hvLen HV calculation period (default: 30)
     * @param annualizationFactor Annualization factor (typically 252 for trading days)
     * @return Annualized HV
     */
    public static Double calculateHV30(List<OHLCV> bars, int hvLen, double annualizationFactor) {
        if (bars == null || bars.size() < hvLen + 1) {
            return null;
        }
        
        double[] logReturns = new double[hvLen];
        for (int i = 0; i < hvLen; i++) {
            double current = bars.get(i).getClose();
            double previous = bars.get(i + 1).getClose();
            if (previous == 0) return null;
            logReturns[i] = Math.log(current / previous);
        }
        
        double stdev = calculateStandardDeviation(logReturns);
        return stdev * Math.sqrt(annualizationFactor);
    }
    
    /**
     * Calculates HV percentile over rankLen period.
     * 
     * @param currentHv Current HV value
     * @param hvHistory Historical HV values (most recent first)
     * @param rankLen Percentile lookback period (default: 252)
     * @return HV percentile (0-100)
     */
    public static Double calculateHVPercentile(double currentHv, List<Double> hvHistory, int rankLen) {
        return calculatePercentile(currentHv, hvHistory, rankLen);
    }

    // ============================================
    // 4. Correlation with SPX
    // ============================================
    
    /**
     * Calculates correlation between stock and SPX over specified period.
     * ThinkScript: def corr = Correlation(close, close("SPX"), corrLen)
     * 
     * @param stockBars Stock OHLCV bars (most recent first)
     * @param spxBars SPX OHLCV bars (most recent first)
     * @param corrLen Correlation period (default: 30)
     * @return Correlation coefficient (-1 to 1)
     */
    public static Double calculateSPXCorrelation(
            List<OHLCV> stockBars, 
            List<OHLCV> spxBars, 
            int corrLen) {
        
        if (stockBars == null || spxBars == null || 
            stockBars.size() < corrLen || spxBars.size() < corrLen) {
            return null;
        }
        
        double[] stockReturns = new double[corrLen];
        double[] spxReturns = new double[corrLen];
        
        for (int i = 0; i < corrLen; i++) {
            double stockPrev = stockBars.get(i + 1).getClose();
            double stockCurr = stockBars.get(i).getClose();
            double spxPrev = spxBars.get(i + 1).getClose();
            double spxCurr = spxBars.get(i).getClose();
            
            if (stockPrev == 0 || spxPrev == 0) return null;
            
            stockReturns[i] = Math.log(stockCurr / stockPrev);
            spxReturns[i] = Math.log(spxCurr / spxPrev);
        }
        
        return new PearsonsCorrelation().correlation(stockReturns, spxReturns);
    }

    // ============================================
    // 5. ATM Vega - 30-day ATM vega calculation
    // ============================================
    
    /**
     * Calculates ATM Vega for 30-day options.
     * Vega represents the sensitivity of option price to IV changes.
     * 
     * ThinkScript equivalent uses option chain data:
     * def atmVega = optionChain().vega() for ATM strike
     * 
     * @param underlyingPrice Current underlying price
     * @param strikePrice ATM strike price (typically closest to underlying)
     * @param timeToExpiry Time to expiry in years (30 days = 30/365)
     * @param iv Implied volatility
     * @param riskFreeRate Risk-free rate (e.g., 0.04 for 4%)
     * @return ATM Vega value
     */
    public static Double calculateATMVega(
            double underlyingPrice,
            double strikePrice,
            double timeToExpiry,
            double iv,
            double riskFreeRate) {
        
        if (underlyingPrice <= 0 || timeToExpiry <= 0 || iv <= 0) {
            return null;
        }
        
        double d1 = calculateD1(underlyingPrice, strikePrice, timeToExpiry, iv, riskFreeRate);
        double nd1 = standardNormalPDF(d1);
        
        // Vega = S * sqrt(T) * N'(d1) * 0.01 (for 1% IV change)
        return underlyingPrice * Math.sqrt(timeToExpiry) * nd1 * 0.01;
    }
    
    /**
     * Calculates ATM Vega percentile over lookback period.
     * 
     * @param currentVega Current ATM vega value
     * @param vegaHistory Historical vega values (most recent first)
     * @param rankLen Percentile lookback period
     * @return Vega percentile (0-100)
     */
    public static Double calculateVegaPercentile(double currentVega, List<Double> vegaHistory, int rankLen) {
        return calculatePercentile(currentVega, vegaHistory, rankLen);
    }

    // ============================================
    // Helper methods
    // ============================================
    
    /**
     * Calculates Simple Moving Average.
     * ThinkScript: Average(data, length)
     */
    private static double calculateSMA(double[] data, int length) {
        if (data.length < length) return 0;
        return StatUtils.mean(data, 0, length);
    }
    
    /**
     * Calculates standard deviation.
     * ThinkScript: StDev(data, length)
     */
    private static double calculateStandardDeviation(double[] data) {
        return Math.sqrt(StatUtils.variance(data));
    }
    
    /**
     * Calculates percentile of a value in a historical series.
     */
    private static Double calculatePercentile(double currentValue, List<Double> history, int length) {
        if (history == null || history.size() < length) {
            return null;
        }
        
        List<Double> relevant = new ArrayList<>(history.subList(0, Math.min(length, history.size())));
        Collections.sort(relevant);
        
        int rank = 0;
        for (Double val : relevant) {
            if (val < currentValue) rank++;
        }
        
        return (relevant.size() == 0) ? 50.0 : (100.0 * rank / relevant.size());
    }
    
    /**
     * Calculate d1 for Black-Scholes.
     */
    private static double calculateD1(double S, double K, double T, double sigma, double r) {
        return (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * Math.sqrt(T));
    }
    
    /**
     * Standard normal probability density function.
     */
    private static double standardNormalPDF(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }
}
