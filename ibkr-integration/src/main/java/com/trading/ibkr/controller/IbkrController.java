package com.trading.ibkr.controller;

import com.trading.model.marketdata.MarketData;
import com.trading.model.marketdata.OptionsChain;
import com.trading.ibkr.service.IbkrMarketDataServiceImpl.OrderRequest;
import com.trading.ibkr.service.IbkrMarketDataServiceImpl.OrderStatus;
import com.trading.ibkr.service.MarketDataService;
import com.trading.ibkr.service.OptionsDataService;
import com.trading.ibkr.service.RecommendationService;
import com.trading.ibkr.service.StrategyService.StrategyRecommendation;
import com.trading.ibkr.service.RecommendationService.TradeRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST API Controller for IBKR integration.
 *
 * <p>Exposes endpoints for:
 * <ul>
 *   <li>Market data retrieval</li>
 *   <li>Options chain data</li>
 *   <li>Trade recommendations</li>
 *   <li>Strategy selection</li>
 *   <li>Order placement (paper trading)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/ibkr")
@RequiredArgsConstructor
public class IbkrController {

    private final MarketDataService marketDataService;
    private final OptionsDataService optionsDataService;
    private final RecommendationService recommendationService;

    /**
     * Gets real-time market data for a symbol.
     *
     * @param symbol the stock symbol (e.g., "AAPL", "SPY")
     * @return Market data
     */
    @GetMapping("/market-data/{symbol}")
    public Mono<ResponseEntity<MarketData>> getMarketData(@PathVariable String symbol) {
        log.info("GET /api/ibkr/market-data/{}", symbol);
        return marketDataService.getMarketData(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets options chain for an underlying symbol.
     *
     * @param symbol the underlying symbol (e.g., "AAPL", "SPY")
     * @return Options chain
     */
    @GetMapping("/options-chain/{symbol}")
    public Mono<ResponseEntity<OptionsChain>> getOptionsChain(@PathVariable String symbol) {
        log.info("GET /api/ibkr/options-chain/{}", symbol);
        return optionsDataService.getOptionsChain(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets near-the-money options chain.
     *
     * @param symbol the underlying symbol
     * @return Filtered options chain
     */
    @GetMapping("/options-chain/{symbol}/near-the-money")
    public Mono<ResponseEntity<OptionsChain>> getNearTheMoneyChain(@PathVariable String symbol) {
        log.info("GET /api/ibkr/options-chain/{}/near-the-money", symbol);
        return optionsDataService.getNearTheMoneyChain(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets optimal expirations for condor strategies.
     *
     * @param symbol the underlying symbol
     * @return Options chain filtered for condor strategies
     */
    @GetMapping("/options-chain/{symbol}/condor-optimized")
    public Mono<ResponseEntity<OptionsChain>> getCondorOptimizedChain(@PathVariable String symbol) {
        log.info("GET /api/ibkr/options-chain/{}/condor-optimized", symbol);
        return optionsDataService.getOptimalCondorExpirations(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Generates a trade recommendation for a symbol.
     *
     * @param symbol the underlying symbol (e.g., "AAPL", "TSLA", "SPY")
     * @return Trade recommendation
     */
    @GetMapping("/recommendation/{symbol}")
    public Mono<ResponseEntity<TradeRecommendation>> getRecommendation(@PathVariable String symbol) {
        log.info("GET /api/ibkr/recommendation/{}", symbol);
        return recommendationService.generateRecommendation(symbol.toUpperCase())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets recommendations for multiple symbols.
     *
     * @param symbols comma-separated list of symbols
     * @return List of trade recommendations
     */
    @GetMapping("/recommendations")
    public Flux<TradeRecommendation> getRecommendations(@RequestParam String symbols) {
        log.info("GET /api/ibkr/recommendations?symbols={}", symbols);
        String[] symbolArray = symbols.split(",");
        return Flux.fromArray(symbolArray)
                .map(String::trim)
                .map(String::toUpperCase)
                .flatMap(recommendationService::generateRecommendation);
    }

    /**
     * Health check endpoint.
     *
     * @return Connection status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthStatus>> health() {
        return Mono.just(ResponseEntity.ok(new HealthStatus("UP", "IBKR Integration Service")));
    }

    // DTOs

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HealthStatus {
        private String status;
        private String service;
    }
}
