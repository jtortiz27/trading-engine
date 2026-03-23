package com.trading.ibkr.service;

import com.trading.ibkr.service.OptionsDataService.OptionsEdgeData;
import com.trading.ibkr.service.StrategyService.StrategyRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that generates trade recommendations by wiring together:
 *
 * <p>Flow:
 * <ol>
 *   <li>IBKR Client → OptionsDataService</li>
 *   <li>OptionsDataService → Options Edge calculations</li>
 *   <li>Options Edge → Strategy selector (Condor/Fly/Spread)</li>
 *   <li>Strategy → Trade recommendation with sizing</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MarketDataService marketDataService;
    private final OptionsDataService optionsDataService;
    private final StrategyService strategyService;

    // Risk management constants
    private static final double MAX_POSITION_SIZE = 10000.0; // $10K max per trade
    private static final double DEFAULT_POSITION_SIZE = 1000.0; // $1K default
    private static final double CONFIDENCE_MULTIPLIER = 100.0; // $100 per confidence point

    /**
     * Generates a complete trade recommendation for a symbol.
     *
     * @param symbol the underlying symbol (e.g., "AAPL", "TSLA", "SPY")
     * @return Mono emitting TradeRecommendation
     */
    public Mono<TradeRecommendation> generateRecommendation(String symbol) {
        log.info("Generating trade recommendation for {}", symbol);

        return optionsDataService.getOptionsEdgeData(symbol)
                .flatMap(strategyService::recommendStrategy)
                .flatMap(rec -> buildTradeRecommendation(symbol, rec))
                .doOnSuccess(rec -> log.info("Generated recommendation for {}: {} with {} confidence",
                        symbol, rec.getStrategy().getStrategyType(), rec.getStrategy().getConfidence()))
                .doOnError(error -> log.error("Failed to generate recommendation for {}: {}",
                        symbol, error.getMessage()));
    }

    /**
     * Builds the complete trade recommendation with sizing.
     */
    private Mono<TradeRecommendation> buildTradeRecommendation(String symbol, StrategyRecommendation strategy) {
        return Mono.fromCallable(() -> {
            // Calculate position size based on confidence
            double positionSize = calculatePositionSize(strategy.getConfidence());

            // Calculate strikes based on strategy
            List<Leg> legs = buildStrategyLegs(strategy);

            // Calculate expected profit and risk
            double expectedProfit = calculateExpectedProfit(strategy, positionSize);
            double maxRisk = calculateMaxRisk(strategy, positionSize);

            // Build recommendation
            return TradeRecommendation.builder()
                    .symbol(symbol)
                    .underlyingPrice(strategy.getUnderlyingPrice())
                    .strategy(strategy)
                    .legs(legs)
                    .positionSize(positionSize)
                    .expectedProfit(expectedProfit)
                    .maxRisk(maxRisk)
                    .riskRewardRatio(maxRisk > 0 ? expectedProfit / maxRisk : 0)
                    .ivPercentile(strategy.getIvPercentile())
                    .sentiment(strategy.getSentiment())
                    .timestamp(LocalDate.now())
                    .build();
        });
    }

    /**
     * Calculates position size based on confidence score.
     */
    private double calculatePositionSize(double confidence) {
        double size = DEFAULT_POSITION_SIZE + (confidence * CONFIDENCE_MULTIPLIER);
        return Math.min(size, MAX_POSITION_SIZE);
    }

    /**
     * Builds strategy legs based on strategy type.
     */
    private List<Leg> buildStrategyLegs(StrategyRecommendation strategy) {
        List<Leg> legs = new ArrayList<>();
        double price = strategy.getUnderlyingPrice();

        switch (strategy.getStrategyType()) {
            case IRON_CONDOR -> {
                // Short put spread + Short call spread
                legs.add(new Leg(LegType.SHORT_PUT, price * 0.95, price * 0.90));
                legs.add(new Leg(LegType.SHORT_CALL, price * 1.05, price * 1.10));
            }
            case BUTTERFLY -> {
                // Buy wing + Sell 2x ATM + Buy wing
                legs.add(new Leg(LegType.LONG_CALL, price * 0.90, 0));
                legs.add(new Leg(LegType.SHORT_CALL, price, 2));
                legs.add(new Leg(LegType.LONG_CALL, price * 1.10, 0));
            }
            case CREDIT_SPREAD -> {
                if ("BULLISH".equals(strategy.getSentiment())) {
                    // Bull put spread
                    legs.add(new Leg(LegType.SHORT_PUT, price * 0.95, 0));
                    legs.add(new Leg(LegType.LONG_PUT, price * 0.90, 0));
                } else {
                    // Bear call spread
                    legs.add(new Leg(LegType.SHORT_CALL, price * 1.05, 0));
                    legs.add(new Leg(LegType.LONG_CALL, price * 1.10, 0));
                }
            }
            case DEBIT_SPREAD -> {
                if ("BULLISH".equals(strategy.getSentiment())) {
                    // Bull call spread
                    legs.add(new Leg(LegType.LONG_CALL, price * 1.00, 0));
                    legs.add(new Leg(LegType.SHORT_CALL, price * 1.05, 0));
                } else {
                    // Bear put spread
                    legs.add(new Leg(LegType.LONG_PUT, price * 1.00, 0));
                    legs.add(new Leg(LegType.SHORT_PUT, price * 0.95, 0));
                }
            }
            case STRADDLE -> {
                // Long ATM call + Long ATM put
                legs.add(new Leg(LegType.LONG_CALL, price, 0));
                legs.add(new Leg(LegType.LONG_PUT, price, 0));
            }
            default -> {
                // No recommendation
            }
        }

        return legs;
    }

    /**
     * Calculates expected profit (simplified estimate).
     */
    private double calculateExpectedProfit(StrategyRecommendation strategy, double positionSize) {
        return switch (strategy.getStrategyType()) {
            case IRON_CONDOR -> positionSize * 0.15 * (strategy.getConfidence() / 100);
            case BUTTERFLY -> positionSize * 0.50 * (strategy.getConfidence() / 100);
            case CREDIT_SPREAD -> positionSize * 0.10 * (strategy.getConfidence() / 100);
            case DEBIT_SPREAD -> positionSize * 0.20 * (strategy.getConfidence() / 100);
            case STRADDLE -> positionSize * 0.30 * (strategy.getConfidence() / 100);
            default -> 0;
        };
    }

    /**
     * Calculates maximum risk (simplified estimate).
     */
    private double calculateMaxRisk(StrategyRecommendation strategy, double positionSize) {
        return switch (strategy.getStrategyType()) {
            case IRON_CONDOR -> positionSize * 0.50; // Wing width - credit
            case BUTTERFLY -> positionSize * 0.10; // Max loss is debit paid
            case CREDIT_SPREAD -> positionSize * 0.20; // Wing width - credit
            case DEBIT_SPREAD -> positionSize * 0.10; // Max loss is debit paid
            case STRADDLE -> positionSize * 1.00; // Full premium
            default -> positionSize;
        };
    }

    /**
     * Trade recommendation data transfer object.
     */
    @lombok.Data
    @lombok.Builder
    public static class TradeRecommendation {
        private String symbol;
        private double underlyingPrice;
        private StrategyRecommendation strategy;
        private List<Leg> legs;
        private double positionSize;
        private double expectedProfit;
        private double maxRisk;
        private double riskRewardRatio;
        private double ivPercentile;
        private String sentiment;
        private LocalDate timestamp;

        public boolean isHighQuality() {
            return strategy.isHighConfidence() && riskRewardRatio >= 1.5;
        }

        public boolean isMediumQuality() {
            return strategy.isMediumConfidence() && riskRewardRatio >= 1.0;
        }

        public String getQualityLabel() {
            if (isHighQuality()) return "HIGH";
            if (isMediumQuality()) return "MEDIUM";
            return "LOW";
        }
    }

    /**
     * Strategy leg definition.
     */
    @lombok.Data
    public static class Leg {
        private final LegType type;
        private final double strike;
        private final int quantity;

        public Leg(LegType type, double strike, int quantity) {
            this.type = type;
            this.strike = strike;
            this.quantity = quantity > 0 ? quantity : 1;
        }
    }

    public enum LegType {
        LONG_CALL,
        SHORT_CALL,
        LONG_PUT,
        SHORT_PUT
    }
}
