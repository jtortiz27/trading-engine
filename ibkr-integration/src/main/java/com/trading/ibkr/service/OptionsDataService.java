package com.trading.ibkr.service;

import com.trading.api.resource.marketdata.OptionContract;
import com.trading.api.resource.marketdata.OptionsChain;
import com.trading.client.IbkrMarketDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for fetching and processing options chain data.
 *
 * <p>Provides:
 * <ul>
 *   <li>Options chain retrieval from IBKR</li>
 *   <li>Greeks enrichment (if not provided)</li>
 *   <li>Filtered views (near-the-money, by expiry)</li>
 *   <li>Data feeding to Options Edge calculations</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptionsDataService {

    private final IbkrMarketDataClient ibkrClient;

    private static final double NTM_PERCENTAGE = 0.10; // Near-the-money threshold (10%)
    private static final int MAX_EXPIRIES = 6; // Max expiration dates to return

    /**
     * Fetches complete options chain for an underlying symbol.
     *
     * @param underlying the underlying symbol (e.g., "AAPL", "SPY")
     * @return Mono emitting OptionsChain
     */
    public Mono<OptionsChain> getOptionsChain(String underlying) {
        log.info("Fetching options chain for {}", underlying);
        return ibkrClient.getOptionsChain(underlying)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalStateException))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying options chain fetch for {} (attempt {})",
                                        underlying, retrySignal.totalRetries() + 1)))
                .doOnSuccess(chain -> log.info("Retrieved options chain for {}: {} calls, {} puts",
                        underlying, chain.getCalls().size(), chain.getPuts().size()))
                .doOnError(error -> log.error("Failed to get options chain for {}: {}",
                        underlying, error.getMessage()));
    }

    /**
     * Gets near-the-money options chain (strikes within 10% of underlying price).
     *
     * @param underlying the underlying symbol
     * @return Mono emitting filtered OptionsChain
     */
    public Mono<OptionsChain> getNearTheMoneyChain(String underlying) {
        return getOptionsChain(underlying)
                .map(chain -> {
                    double underlyingPrice = chain.getUnderlyingPrice();
                    double lowerStrike = underlyingPrice * (1 - NTM_PERCENTAGE);
                    double upperStrike = underlyingPrice * (1 + NTM_PERCENTAGE);

                    List<OptionContract> filteredCalls = chain.getCalls().stream()
                            .filter(c -> c.getStrike() >= lowerStrike && c.getStrike() <= upperStrike)
                            .collect(Collectors.toList());

                    List<OptionContract> filteredPuts = chain.getPuts().stream()
                            .filter(c -> c.getStrike() >= lowerStrike && c.getStrike() <= upperStrike)
                            .collect(Collectors.toList());

                    return OptionsChain.builder()
                            .underlying(chain.getUnderlying())
                            .underlyingPrice(chain.getUnderlyingPrice())
                            .calls(filteredCalls)
                            .puts(filteredPuts)
                            .timestamp(chain.getTimestamp())
                            .build();
                });
    }

    /**
     * Gets options chain filtered by specific expiration date.
     *
     * @param underlying the underlying symbol
     * @param expiryDate the expiration date
     * @return Mono emitting filtered OptionsChain
     */
    public Mono<OptionsChain> getOptionsChainByExpiry(String underlying, LocalDate expiryDate) {
        return getOptionsChain(underlying)
                .map(chain -> {
                    List<OptionContract> filteredCalls = chain.getCalls().stream()
                            .filter(c -> c.getExpiry().equals(expiryDate))
                            .sorted(Comparator.comparing(OptionContract::getStrike))
                            .collect(Collectors.toList());

                    List<OptionContract> filteredPuts = chain.getPuts().stream()
                            .filter(c -> c.getExpiry().equals(expiryDate))
                            .sorted(Comparator.comparing(OptionContract::getStrike))
                            .collect(Collectors.toList());

                    return OptionsChain.builder()
                            .underlying(chain.getUnderlying())
                            .underlyingPrice(chain.getUnderlyingPrice())
                            .calls(filteredCalls)
                            .puts(filteredPuts)
                            .timestamp(chain.getTimestamp())
                            .build();
                });
    }

    /**
     * Gets the best expiration dates for condor/fly strategies.
     * Prefers expirations between 20-45 days out.
     *
     * @param underlying the underlying symbol
     * @return Mono emitting OptionsChain with optimal expirations
     */
    public Mono<OptionsChain> getOptimalCondorExpirations(String underlying) {
        return getOptionsChain(underlying)
                .map(chain -> {
                    LocalDate today = LocalDate.now();
                    LocalDate minExpiry = today.plusDays(20);
                    LocalDate maxExpiry = today.plusDays(45);

                    List<OptionContract> filteredCalls = chain.getCalls().stream()
                            .filter(c -> !c.getExpiry().isBefore(minExpiry) && !c.getExpiry().isAfter(maxExpiry))
                            .sorted(Comparator.comparing(OptionContract::getExpiry)
                                    .thenComparing(OptionContract::getStrike))
                            .collect(Collectors.toList());

                    List<OptionContract> filteredPuts = chain.getPuts().stream()
                            .filter(c -> !c.getExpiry().isBefore(minExpiry) && !c.getExpiry().isAfter(maxExpiry))
                            .sorted(Comparator.comparing(OptionContract::getExpiry)
                                    .thenComparing(OptionContract::getStrike))
                            .collect(Collectors.toList());

                    return OptionsChain.builder()
                            .underlying(chain.getUnderlying())
                            .underlyingPrice(chain.getUnderlyingPrice())
                            .calls(filteredCalls)
                            .puts(filteredPuts)
                            .timestamp(chain.getTimestamp())
                            .build();
                });
    }

    /**
     * Calculates implied volatility percentile from options data.
     * Used for Options Edge calculations.
     *
     * @param underlying the underlying symbol
     * @return Mono emitting IV percentile (0-100)
     */
    public Mono<Double> calculateIVPercentile(String underlying) {
        return getNearTheMoneyChain(underlying)
                .map(chain -> {
                    List<Double> ivs = chain.getCalls().stream()
                            .map(OptionContract::getImpliedVolatility)
                            .filter(iv -> iv != null && iv > 0)
                            .collect(Collectors.toList());

                    if (ivs.isEmpty()) {
                        return 50.0; // Default middle value
                    }

                    double avgIV = ivs.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.2);

                    // This would typically compare to historical IV range
                    // For now, return a normalized value
                    return Math.min(100, Math.max(0, avgIV * 400));
                });
    }

    /**
     * Gets the ATM straddle price for volatility assessment.
     *
     * @param underlying the underlying symbol
     * @return Mono emitting ATM straddle price
     */
    public Mono<Double> getATMStraddlePrice(String underlying) {
        return getOptionsChain(underlying)
                .map(chain -> {
                    double underlyingPrice = chain.getUnderlyingPrice();

                    // Find closest strike to underlying price
                    OptionContract atmCall = chain.getCalls().stream()
                            .min(Comparator.comparingDouble(c -> Math.abs(c.getStrike() - underlyingPrice)))
                            .orElse(null);

                    OptionContract atmPut = chain.getPuts().stream()
                            .min(Comparator.comparingDouble(c -> Math.abs(c.getStrike() - underlyingPrice)))
                            .orElse(null);

                    if (atmCall == null || atmPut == null) {
                        return underlyingPrice * 0.05; // Default estimate: 5% of underlying
                    }

                    return atmCall.getLastPrice() + atmPut.getLastPrice();
                });
    }

    /**
     * Feeds options data to Options Edge calculations.
     * Returns a processed data structure suitable for strategy selection.
     *
     * @param underlying the underlying symbol
     * @return Mono emitting OptionsEdgeData
     */
    public Mono<OptionsEdgeData> getOptionsEdgeData(String underlying) {
        return Mono.zip(
                        getNearTheMoneyChain(underlying),
                        calculateIVPercentile(underlying),
                        getATMStraddlePrice(underlying)
                )
                .map(tuple -> {
                    OptionsChain chain = tuple.getT1();
                    double ivPercentile = tuple.getT2();
                    double atmStraddle = tuple.getT3();

                    return OptionsEdgeData.builder()
                            .symbol(underlying)
                            .underlyingPrice(chain.getUnderlyingPrice())
                            .ivPercentile(ivPercentile)
                            .atmStraddlePrice(atmStraddle)
                            .totalCallVolume(chain.getCalls().stream()
                                    .mapToLong(OptionContract::getVolume)
                                    .sum())
                            .totalPutVolume(chain.getPuts().stream()
                                    .mapToLong(OptionContract::getVolume)
                                    .sum())
                            .avgCallIV(chain.getCalls().stream()
                                    .mapToDouble(OptionContract::getImpliedVolatility)
                                    .filter(iv -> iv > 0)
                                    .average()
                                    .orElse(0.2))
                            .avgPutIV(chain.getPuts().stream()
                                    .mapToDouble(OptionContract::getImpliedVolatility)
                                    .filter(iv -> iv > 0)
                                    .average()
                                    .orElse(0.2))
                            .timestamp(chain.getTimestamp())
                            .build();
                });
    }

    /**
     * Data transfer object for Options Edge calculations.
     */
    @lombok.Data
    @lombok.Builder
    public static class OptionsEdgeData {
        private String symbol;
        private double underlyingPrice;
        private double ivPercentile;
        private double atmStraddlePrice;
        private long totalCallVolume;
        private long totalPutVolume;
        private double avgCallIV;
        private double avgPutIV;
        private LocalDate timestamp;

        /**
         * Calculates put/call ratio
         */
        public double getPutCallRatio() {
            if (totalCallVolume == 0) return 1.0;
            return (double) totalPutVolume / totalCallVolume;
        }

        /**
         * Determines if market sentiment is bullish or bearish
         */
        public String getSentiment() {
            double ratio = getPutCallRatio();
            if (ratio > 1.2) return "BEARISH";
            if (ratio < 0.8) return "BULLISH";
            return "NEUTRAL";
        }
    }
}
