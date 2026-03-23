package com.trading.ibkr.service;

import com.trading.model.marketdata.MarketData;
import com.trading.client.IbkrMarketDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching and managing market data for underlying securities.
 *
 * <p>Provides:
 * <ul>
 *   <li>Real-time tick data from IBKR</li>
 *   <li>Cached data with fallback</li>
 *   <li>Streaming updates via Flux</li>
 *   <li>VIX data retrieval</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final IbkrMarketDataClient ibkrClient;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final ConcurrentHashMap<String, CachedMarketData> cache = new ConcurrentHashMap<>();

    /**
     * Fetches current market data for a symbol.
     *
     * @param symbol the stock symbol
     * @return Mono emitting MarketData
     */
    public Mono<MarketData> getMarketData(String symbol) {
        // Check cache first
        CachedMarketData cached = cache.get(symbol);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached market data for {}", symbol);
            return Mono.just(cached.getData());
        }

        log.info("Fetching market data for {}", symbol);
        return ibkrClient.getMarketData(symbol)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalStateException))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying market data fetch for {} (attempt {})",
                                        symbol, retrySignal.totalRetries() + 1)))
                .doOnSuccess(data -> {
                    cache.put(symbol, new CachedMarketData(data));
                    log.info("Retrieved market data for {}: lastPrice={}, bid={}, ask={}",
                            symbol, data.getLastPrice(), data.getBidPrice(), data.getAskPrice());
                })
                .doOnError(error -> log.error("Failed to get market data for {}: {}",
                        symbol, error.getMessage()));
    }

    /**
     * Gets current VIX index value.
     *
     * @return Mono emitting VIX value
     */
    public Mono<Double> getVIX() {
        return getMarketData("VIX")
                .map(MarketData::getVix)
                .defaultIfEmpty(20.0); // Default VIX if unavailable
    }

    /**
     * Gets bid-ask spread for a symbol.
     *
     * @param symbol the stock symbol
     * @return Mono emitting spread (ask - bid)
     */
    public Mono<Double> getSpread(String symbol) {
        return getMarketData(symbol)
                .map(data -> data.getAskPrice() - data.getBidPrice())
                .filter(spread -> spread >= 0)
                .defaultIfEmpty(0.0);
    }

    /**
     * Gets mid price for a symbol.
     *
     * @param symbol the stock symbol
     * @return Mono emitting mid price
     */
    public Mono<Double> getMidPrice(String symbol) {
        return getMarketData(symbol)
                .map(data -> (data.getBidPrice() + data.getAskPrice()) / 2.0);
    }

    /**
     * Calculates liquidity score based on spread and volume.
     * Higher score = more liquid.
     *
     * @param symbol the stock symbol
     * @return Mono emitting liquidity score (0-100)
     */
    public Mono<Double> getLiquidityScore(String symbol) {
        return getMarketData(symbol)
                .map(data -> {
                    if (data.getLastPrice() == null || data.getVolume() == null) {
                        return 50.0;
                    }

                    // Spread as percentage of price
                    double spreadPct = (data.getAskPrice() - data.getBidPrice()) / data.getLastPrice() * 100;

                    // Volume score (logarithmic)
                    double volumeScore = Math.log10(data.getVolume()) / 7.0 * 100; // 10M = 100

                    // Combined score: lower spread = higher score, higher volume = higher score
                    double spreadScore = Math.max(0, 100 - spreadPct * 20); // 5% spread = 0 score

                    return (spreadScore * 0.6) + (volumeScore * 0.4);
                })
                .defaultIfEmpty(50.0);
    }

    /**
     * Streams market data updates for a symbol.
     * Note: In full implementation, this would use IBKR's streaming API.
     * For now, polls every second.
     *
     * @param symbol the stock symbol
     * @return Flux emitting MarketData updates
     */
    public Flux<MarketData> streamMarketData(String symbol) {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .flatMap(tick -> getMarketData(symbol)
                        .onErrorResume(error -> {
                            log.debug("Error streaming market data for {}: {}", symbol, error.getMessage());
                            return Mono.empty();
                        }))
                .distinctUntilChanged();
    }

    /**
     * Gets multiple symbols' market data in parallel.
     *
     * @param symbols array of stock symbols
     * @return Flux emitting MarketData for each symbol
     */
    public Flux<MarketData> getMarketDataBatch(String... symbols) {
        return Flux.fromArray(symbols)
                .flatMap(this::getMarketData);
    }

    /**
     * Clears the market data cache.
     */
    public void clearCache() {
        log.info("Clearing market data cache");
        cache.clear();
    }

    /**
     * Gets cached data age for a symbol.
     *
     * @param symbol the stock symbol
     * @return Duration since last update, or null if not cached
     */
    public Duration getCacheAge(String symbol) {
        CachedMarketData cached = cache.get(symbol);
        if (cached == null) {
            return null;
        }
        return Duration.between(cached.getTimestamp(), java.time.Instant.now());
    }

    /**
     * Inner class for cached market data with TTL.
     */
    private static class CachedMarketData {
        private final MarketData data;
        private final java.time.Instant timestamp;

        CachedMarketData(MarketData data) {
            this.data = data;
            this.timestamp = java.time.Instant.now();
        }

        MarketData getData() {
            return data;
        }

        java.time.Instant getTimestamp() {
            return timestamp;
        }

        boolean isExpired() {
            return Duration.between(timestamp, java.time.Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
