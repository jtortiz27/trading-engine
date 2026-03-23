package com.trading.model.options.analytics;

import com.trading.model.options.data.ExpirationSlice;
import com.trading.model.options.data.OptionStrike;
import com.trading.model.options.data.OptionsChainData;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Main service for options analytics calculations.
 * 
 * Provides:
 * - Comprehensive options analysis
 * - Trade idea generation
 * - Risk metrics calculation
 */
@Service
@RequiredArgsConstructor
public class OptionsAnalyticsService {
    
    private final GammaDeltaFlowCalculator gexCalculator = new GammaDeltaFlowCalculator();
    private final OpenInterestCalculator oiCalculator = new OpenInterestCalculator();
    private final VolSurfaceCalculator volCalculator = new VolSurfaceCalculator();
    private final RealizedVolCalculator rvCalculator = new RealizedVolCalculator();
    private final LiquidityCalculator liquidityCalculator = new LiquidityCalculator();
    private final FlyRankingEngine flyRankingEngine = new FlyRankingEngine();
    
    /**
     * Performs full analytics on an options chain.
     */
    public AnalyticsReport analyze(OptionsChainData chainData) {
        AnalyticsReport report = new AnalyticsReport();
        report.setSymbol(chainData.getUnderlyingSymbol());
        report.setSpot(chainData.getUnderlyingPrice());
        report.setTimestamp(LocalDate.now());
        
        // Analyze front month
        ExpirationSlice frontMonth = chainData.getFrontMonth();
        if (frontMonth != null) {
            report.setFrontMonthAnalysis(analyzeExpiration(frontMonth));
        }
        
        // Term structure analysis
        if (chainData.getExpirations() != null && chainData.getExpirations().size() >= 2) {
            report.setTermStructure(volCalculator.calculateTermStructure(chainData.getExpirations()));
        }
        
        // Realized vol
        if (chainData.getHistoricalVol20() != null) {
            RealizedVolCalculator.RealizedVolResult rvResult = new RealizedVolCalculator.RealizedVolResult();
            rvResult.setHv10(chainData.getHistoricalVol10());
            rvResult.setHv20(chainData.getHistoricalVol20());
            report.setRealizedVol(rvResult);
        }
        
        // Generate trade ideas
        report.setTradeIdeas(flyRankingEngine.generateTradeIdeas(chainData));
        
        return report;
    }
    
    /**
     * Analyzes a single expiration slice.
     */
    public ExpirationAnalysis analyzeExpiration(ExpirationSlice slice) {
        ExpirationAnalysis analysis = new ExpirationAnalysis();
        analysis.setExpiryDate(slice.getExpiryDate());
        analysis.setDaysToExpiry(slice.getDaysToExpiry());
        
        // GEX
        analysis.setGex(gexCalculator.calculate(slice));
        
        // OI structure
        analysis.setOiStructure(oiCalculator.calculate(slice));
        
        // Vol surface
        analysis.setVolSurface(volCalculator.calculate(slice));
        
        // Liquidity
        analysis.setLiquidity(liquidityCalculator.calculate(slice));
        
        return analysis;
    }
    
    /**
     * Generates trade ideas from chain data.
     */
    public FlyRankingEngine.RankingResult generateTradeIdeas(OptionsChainData chainData) {
        return flyRankingEngine.generateTradeIdeas(chainData);
    }
    
    /**
     * Calculates quick GEX snapshot.
     */
    public GammaDeltaFlowCalculator.GammaDeltaFlowResult calculateGex(ExpirationSlice slice) {
        return gexCalculator.calculate(slice);
    }
    
    /**
     * Calculates historical volatility.
     */
    public Double calculateHistoricalVolatility(List<Double> closes, int period) {
        return rvCalculator.calculateHv(closes, period);
    }
    
    /**
     * Analytics report container.
     */
    @lombok.Data
    public static class AnalyticsReport {
        private String symbol;
        private Double spot;
        private LocalDate timestamp;
        private ExpirationAnalysis frontMonthAnalysis;
        private VolSurfaceCalculator.TermStructureResult termStructure;
        private RealizedVolCalculator.RealizedVolResult realizedVol;
        private FlyRankingEngine.RankingResult tradeIdeas;
    }
    
    /**
     * Single expiration analysis container.
     */
    @lombok.Data
    public static class ExpirationAnalysis {
        private LocalDate expiryDate;
        private Integer daysToExpiry;
        private GammaDeltaFlowCalculator.GammaDeltaFlowResult gex;
        private OpenInterestCalculator.OpenInterestResult oiStructure;
        private VolSurfaceCalculator.VolSurfaceResult volSurface;
        private LiquidityCalculator.LiquidityResult liquidity;
    }
}
