package com.trading.api;

import com.trading.api.resource.marketdata.MarketData;
import com.trading.api.resource.marketdata.OptionsChain;
import com.trading.api.resource.recommendation.RecommendationResponse;
import com.trading.client.IbkrMarketDataClient;
import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * REST Controller for trade recommendations.
 *
 * <p>Refactored to support new architecture:
 * <ol>
 *   <li>Receives request with symbol</li>
 *   <li>Calls model-sidecar at POST /predict with StockFeatures</li>
 *   <li>Gets recommendation back</li>
 *   <li>Calls IBKR stub for mock market data</li>
 *   <li>Returns enriched response with recommendation + mock market data</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

  private final IbkrMarketDataClient ibkrMarketDataClient;
  private final WebClient.Builder webClientBuilder;

  @Value("${model-sidecar.url:http://localhost:8081}")
  private String modelSidecarUrl;

  private static final Duration MODEL_SIDECAR_TIMEOUT = Duration.ofSeconds(5);

  /**
   * Returns enriched trade recommendation for the given symbol.
   *
   * <p>Flow:
   * <ol>
   *   <li>Calls model-sidecar POST /predict to get ML recommendation</li>
   *   <li>Fetches market data from IBKR stub</li>
   *   <li>Fetches options chain from IBKR stub</li>
   *   <li>Combines into enriched response</li>
   * </ol>
   *
   * <p>Error handling:
   * <ul>
   *   <li>Model-sidecar unavailable → 503 Service Unavailable</li>
   *   <li>IBKR stub can return mock errors for testing (controlled by parameter)</li>
   * </ul>
   *
   * @param symbol the stock symbol to analyze (e.g., "AAPL", "SPY")
   * @param mockError if true, simulates IBKR error for testing (optional, default false)
   * @return Mono emitting enriched RecommendationResponse
   */
  @GetMapping
  public Mono<RecommendationResponse> getRecommendation(
      @RequestParam String symbol,
      @RequestParam(required = false, defaultValue = "false") boolean mockError) {

    log.info("Processing recommendation request for symbol: {}", symbol);

    // Step 1: Call model-sidecar for prediction
    Mono<TradeRecommendation> recommendationMono = callModelSidecar(symbol)
        .timeout(MODEL_SIDECAR_TIMEOUT)
        .onErrorMap(this::handleModelSidecarError);

    // Step 2: Get market data (with optional error simulation)
    Mono<MarketData> marketDataMono = mockError
        ? ibkrMarketDataClient.getMarketDataWithError(symbol)
        : ibkrMarketDataClient.getMarketData(symbol);

    // Step 3: Get options chain
    Mono<OptionsChain> optionsChainMono = ibkrMarketDataClient.getOptionsChain(symbol);

    // Combine all three reactive streams
    return Mono.zip(recommendationMono, marketDataMono, optionsChainMono)
        .map(tuple -> RecommendationResponse.from(
            tuple.getT1(),
            tuple.getT2(),
            tuple.getT3()))
        .doOnSuccess(response ->
            log.info("Successfully generated recommendation for {}: {} (confidence: {})",
                symbol, response.getRecommendation(), response.getConfidence()))
        .doOnError(error ->
            log.error("Failed to generate recommendation for {}: {}", symbol, error.getMessage()));
  }

  /**
   * Calls the model-sidecar service to get ML prediction.
   *
   * @param symbol the stock symbol
   * @return Mono emitting TradeRecommendation from model
   */
  private Mono<TradeRecommendation> callModelSidecar(String symbol) {
    // Create minimal StockFeatures for the stub call
    // In production, this would include full feature set
    StockFeatures features = StockFeatures.builder()
        .symbol(symbol)
        .build();

    WebClient modelClient = webClientBuilder
        .baseUrl(modelSidecarUrl)
        .build();

    return modelClient.post()
        .uri("/predict")
        .bodyValue(features)
        .retrieve()
        .bodyToMono(TradeRecommendation.class);
  }

  /**
   * Maps model-sidecar errors to appropriate exceptions.
   *
   * @param error the original error
   * @return mapped exception for proper HTTP status
   */
  private Throwable handleModelSidecarError(Throwable error) {
    if (error instanceof WebClientResponseException) {
      WebClientResponseException wcre = (WebClientResponseException) error;
      log.warn("Model-sidecar returned HTTP {}: {}", wcre.getStatusCode(), wcre.getMessage());
      return new RecommendationException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Model-sidecar service unavailable: " + wcre.getStatusCode());
    }
    log.warn("Model-sidecar connection failed: {}", error.getMessage());
    return new RecommendationException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Model-sidecar service unavailable");
  }

  /**
   * Custom exception for recommendation errors with HTTP status.
   */
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public static class RecommendationException extends RuntimeException {
    private final HttpStatus status;

    public RecommendationException(HttpStatus status, String message) {
      super(message);
      this.status = status;
    }

    public HttpStatus getStatus() {
      return status;
    }
  }
}
