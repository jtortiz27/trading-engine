package com.trading.model.marketdata;

import reactor.core.publisher.Mono;

/**
 * Client interface for Interactive Brokers (IBKR) Gateway market data.
 *
 * <p>This interface defines the contract for fetching real-time market data and
 * options chain information from the IBKR Gateway.
 *
 * @see MarketData
 * @see OptionsChain
 */
public interface IbkrMarketDataClient {

  /**
   * Fetches real-time market data for the specified symbol.
   *
   * @param symbol the stock symbol to fetch data for (e.g., "AAPL", "SPY")
   * @return Mono emitting MarketData, or error if request fails
   */
  Mono<MarketData> getMarketData(String symbol);

  /**
   * Fetches options chain data for the specified underlying symbol.
   *
   * @param symbol the underlying stock symbol (e.g., "AAPL", "SPY")
   * @return Mono emitting OptionsChain, or error if request fails
   */
  Mono<OptionsChain> getOptionsChain(String symbol);

  /**
   * Simulates an error response for testing error handling.
   * Useful for validating circuit breakers and fallback behavior.
   *
   * @param symbol the stock symbol
   * @return Mono that emits an error
   */
  default Mono<MarketData> getMarketDataWithError(String symbol) {
    return Mono.error(new RuntimeException("IBKR Gateway error for symbol: " + symbol));
  }
}
