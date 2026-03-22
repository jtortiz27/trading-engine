package com.trading.client;

import com.trading.api.resource.marketdata.MarketData;
import com.trading.api.resource.marketdata.OptionsChain;
import reactor.core.publisher.Mono;

/**
 * Stub client interface for Interactive Brokers (IBKR) Gateway market data.
 *
 * <p>This interface defines the contract for fetching real-time market data and
 * options chain information from the IBKR Gateway. Currently implemented as a stub
 * that returns mock data for development and testing purposes.
 *
 * <p>Future implementation will connect to IBKR Gateway via TWS API on:
 * <ul>
 *   <li>Port 7497 - Paper trading mode</li>
 *   <li>Port 7496 - Live trading mode</li>
 * </ul>
 *
 * @see MarketData
 * @see OptionsChain
 */
public interface IbkrMarketDataClient {

  /**
   * Fetches real-time market data for the specified symbol.
   *
   * <p>Current stub implementation returns mock data including:
   * <ul>
   *   <li>Last trade price</li>
   *   <li>Bid/ask prices</li>
   *   <li>Volume</li>
   *   <li>VIX index (when available)</li>
   * </ul>
   *
   * @param symbol the stock symbol to fetch data for (e.g., "AAPL", "SPY")
   * @return Mono emitting MarketData, or error if stub fails
   */
  Mono<MarketData> getMarketData(String symbol);

  /**
   * Fetches options chain data for the specified underlying symbol.
   *
   * <p>Current stub implementation returns mock data including:
   * <ul>
   *   <li>Available expiration dates</li>
   *   <li>Call and put contracts across strikes</li>
   *   <li>Greeks (delta, gamma, theta, vega, rho)</li>
   *   <li>Implied volatility</li>
   * </ul>
   *
   * @param symbol the underlying stock symbol (e.g., "AAPL", "SPY")
   * @return Mono emitting OptionsChain, or error if stub fails
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
    return Mono.error(new RuntimeException("IBKR Gateway stub error for symbol: " + symbol));
  }
}
