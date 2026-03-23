package com.trading.ibkr.service;

import com.trading.ibkr.service.OptionsDataService.OptionsEdgeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy selection service for options trading.
 *
 * <p>Selects appropriate strategies based on:
 * <ul>
 *   <li>IV percentile (rank)</li>
 *   <li>Market sentiment</li>
 *   <li>Price action</li>
 *   <li>Risk tolerance</li>
 * </ul>
 *
 * <p>Supported strategies:
 * <ul>
 *   <li>Iron Condor - high IV, neutral</li>
 *   <li>Butterfly - low IV, directional</li>
 *   <li>Credit Spread - directional, defined risk</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyService {

    // IV percentile thresholds
    private static final double IV_HIGH_THRESHOLD = 60.0;
    private static final double IV_LOW_THRESHOLD = 40.0;

    // Strategy type enum
    public enum StrategyType {
        IRON_CONDOR,
        BUTTERFLY,
        CREDIT_SPREAD,
        DEBIT_SPREAD,
        STRADDLE,
        NONE
    }

    /**
     * Recommends the best strategy based on market conditions.
     *
     * @param optionsEdgeData the options edge data
     * @return Mono emitting StrategyRecommendation
     */
    public Mono<StrategyRecommendation> recommendStrategy(OptionsEdgeData optionsEdgeData) {
        return Mono.fromCallable(() -> {
            String symbol = optionsEdgeData.getSymbol();
            double ivPercentile = optionsEdgeData.getIvPercentile();
            String sentiment = optionsEdgeData.getSentiment();
            double underlyingPrice = optionsEdgeData.getUnderlyingPrice();

            log.info("Selecting strategy for {}: IV percentile={}, sentiment={}",
                    symbol, ivPercentile, sentiment);

            StrategyType selectedStrategy = selectStrategy(ivPercentile, sentiment);
            double confidence = calculateConfidence(ivPercentile, sentiment, selectedStrategy);
            List<String> reasons = buildReasons(ivPercentile, sentiment, selectedStrategy);

            return StrategyRecommendation.builder()
                    .symbol(symbol)
                    .underlyingPrice(underlyingPrice)
                    .strategyType(selectedStrategy)
                    .confidence(confidence)
                    .reasons(reasons)
                    .ivPercentile(ivPercentile)
                    .sentiment(sentiment)
                    .build();
        });
    }

    /**
     * Selects strategy based on IV and sentiment.
     */
    private StrategyType selectStrategy(double ivPercentile, String sentiment) {
        // High IV environment - favor selling strategies
        if (ivPercentile > IV_HIGH_THRESHOLD) {
            if ("NEUTRAL".equals(sentiment)) {
                return StrategyType.IRON_CONDOR;
            }
            return StrategyType.CREDIT_SPREAD;
        }

        // Low IV environment - favor buying strategies
        if (ivPercentile < IV_LOW_THRESHOLD) {
            if ("NEUTRAL".equals(sentiment)) {
                return StrategyType.STRADDLE;
            }
            return StrategyType.BUTTERFLY;
        }

        // Mid IV environment
        if ("NEUTRAL".equals(sentiment)) {
            return StrategyType.IRON_CONDOR;
        }

        return StrategyType.CREDIT_SPREAD;
    }

    /**
     * Calculates confidence score (0-100) based on signal strength.
     */
    private double calculateConfidence(double ivPercentile, String sentiment, StrategyType strategy) {
        double baseConfidence = 60.0;

        // IV extreme bonus
        if (ivPercentile > 80 || ivPercentile < 20) {
            baseConfidence += 15;
        }

        // Sentiment clarity bonus
        if ("BULLISH".equals(sentiment) || "BEARISH".equals(sentiment)) {
            baseConfidence += 10;
        } else {
            baseConfidence -= 5; // Neutral is less confident
        }

        // Strategy-market fit bonus
        if (isStrategyFit(strategy, ivPercentile, sentiment)) {
            baseConfidence += 10;
        }

        return Math.min(100, Math.max(0, baseConfidence));
    }

    /**
     * Checks if strategy fits market conditions.
     */
    private boolean isStrategyFit(StrategyType strategy, double ivPercentile, String sentiment) {
        return switch (strategy) {
            case IRON_CONDOR -> ivPercentile > IV_HIGH_THRESHOLD && "NEUTRAL".equals(sentiment);
            case BUTTERFLY -> ivPercentile < IV_LOW_THRESHOLD;
            case CREDIT_SPREAD -> ivPercentile > IV_HIGH_THRESHOLD;
            case STRADDLE -> ivPercentile < IV_LOW_THRESHOLD;
            default -> true;
        };
    }

    /**
     * Builds list of reasons for strategy selection.
     */
    private List<String> buildReasons(double ivPercentile, String sentiment, StrategyType strategy) {
        List<String> reasons = new ArrayList<>();

        // IV reason
        if (ivPercentile > IV_HIGH_THRESHOLD) {
            reasons.add(String.format("High IV percentile (%.0f%%) favors premium selling strategies", ivPercentile));
        } else if (ivPercentile < IV_LOW_THRESHOLD) {
            reasons.add(String.format("Low IV percentile (%.0f%%) favors premium buying strategies", ivPercentile));
        } else {
            reasons.add(String.format("Moderate IV percentile (%.0f%%)", ivPercentile));
        }

        // Sentiment reason
        reasons.add(String.format("Market sentiment is %s", sentiment.toLowerCase()));

        // Strategy-specific reason
        reasons.add(getStrategyReason(strategy));

        return reasons;
    }

    private String getStrategyReason(StrategyType strategy) {
        return switch (strategy) {
            case IRON_CONDOR -> "Iron Condor profits from range-bound movement with defined risk";
            case BUTTERFLY -> "Butterfly provides leveraged directional exposure at low cost";
            case CREDIT_SPREAD -> "Credit Spread generates income with defined risk";
            case DEBIT_SPREAD -> "Debit Spread provides directional exposure with limited risk";
            case STRADDLE -> "Long Straddle profits from large moves regardless of direction";
            case NONE -> "No clear strategy recommendation";
        };
    }

    /**
     * Strategy recommendation data transfer object.
     */
    @lombok.Data
    @lombok.Builder
    public static class StrategyRecommendation {
        private String symbol;
        private double underlyingPrice;
        private StrategyType strategyType;
        private double confidence;
        private List<String> reasons;
        private double ivPercentile;
        private String sentiment;

        public boolean isHighConfidence() {
            return confidence >= 75;
        }

        public boolean isMediumConfidence() {
            return confidence >= 50 && confidence < 75;
        }

        public String getDescription() {
            return switch (strategyType) {
                case IRON_CONDOR -> "Iron Condor";
                case BUTTERFLY -> "Butterfly";
                case CREDIT_SPREAD -> "Credit Spread";
                case DEBIT_SPREAD -> "Debit Spread";
                case STRADDLE -> "Long Straddle";
                case NONE -> "None";
            };
        }
    }
}
