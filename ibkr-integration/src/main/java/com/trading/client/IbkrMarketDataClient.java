package com.trading.client;

import com.trading.model.marketdata.MarketData;
import com.trading.model.marketdata.OptionsChain;
import reactor.core.publisher.Mono;

/**
 * Client interface for fetching market data from IBKR.
 */
public interface IbkrMarketDataClient {
    /**
     * Fetches current market data for a symbol.
     *
     * @param symbol the stock symbol
     * @return Mono emitting MarketData
     */
    Mono<MarketData> getMarketData(String symbol);

    /**
     * Fetches options chain for an underlying symbol.
     *
     * @param underlying the underlying symbol (e.g., "AAPL", "SPY")
     * @return Mono emitting OptionsChain
     */
    Mono<OptionsChain> getOptionsChain(String underlying);
}
