package com.trading.client;

import com.trading.api.resource.marketdata.MarketData;
import com.trading.api.resource.marketdata.OptionsChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Stub implementation of {@link IbkrMarketDataClient} for development and testing.
 *
 * <p>Returns mock market data and options chain data without requiring an actual
 * IBKR Gateway connection. Simulates network latency for realistic testing.
 *
 * <p>In production, this will be replaced with a full TWS API implementation
 * connecting to IBKR Gateway on ports 7497 (paper) or 7496 (live).
 */
@Slf4j
@Component
public class IbkrMarketDataClientStub implements IbkrMarketDataClient {

  private static final Duration MOCK_LATENCY = Duration.ofMillis(50);

  /**
   * {@inheritDoc}
   *
   * <p>Returns mock market data with simulated latency.
   */
  @Override
  public Mono<MarketData> getMarketData(String symbol) {
    log.debug("[STUB] Fetching market data for symbol: {}", symbol);
    return Mono.just(MarketData.mock(symbol))
        .delayElement(MOCK_LATENCY)
        .doOnSuccess(data -> log.debug("[STUB] Returning mock market data for {}: lastPrice={}",
            symbol, data.getLastPrice()));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns mock options chain with simulated latency.
   */
  @Override
  public Mono<OptionsChain> getOptionsChain(String symbol) {
    log.debug("[STUB] Fetching options chain for symbol: {}", symbol);
    return Mono.just(OptionsChain.mock(symbol))
        .delayElement(MOCK_LATENCY)
        .doOnSuccess(chain -> log.debug("[STUB] Returning mock options chain for {}: {} calls, {} puts",
            symbol, chain.getCalls().size(), chain.getPuts().size()));
  }
}
