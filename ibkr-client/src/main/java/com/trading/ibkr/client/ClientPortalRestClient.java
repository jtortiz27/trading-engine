package com.trading.ibkr.client;

import com.trading.ibkr.model.Order;
import com.trading.ibkr.model.OptionsChain;
import com.trading.ibkr.model.TickData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive REST client for IBKR Client Portal Web API.
 * Provides headless access without requiring TWS GUI.
 *
 * @see <a href="https://www.interactivebrokers.com/api/doc.html">IBKR Client Portal API Docs</a>
 */
public interface ClientPortalRestClient {

    /**
     * Authenticate and establish session with Client Portal.
     * Must be called before other operations.
     *
     * @param username IBKR username
     * @param password IBKR password
     * @return Mono completing when authenticated
     */
    Mono<Void> authenticate(String username, String password);

    /**
     * Check if currently authenticated.
     *
     * @return Mono with authentication status
     */
    Mono<Boolean> isAuthenticated();

    /**
     * Get list of accounts.
     *
     * @return Mono with list of account IDs
     */
    Mono<List<String>> getAccounts();

    /**
     * Get portfolio positions for the account.
     *
     * @return Mono with list of positions
     */
    Mono<List<Map<String, Object>>> getPortfolio();

    /**
     * Get real-time market data for a symbol.
     * Client Portal API: GET /iserver/marketdata/snapshot
     *
     * @param symbol Stock symbol (e.g., "AAPL")
     * @return Mono with tick data
     */
    Mono<TickData> getMarketData(String symbol);

    /**
     * Stream market data updates via Server-Sent Events.
     *
     * @param symbol Stock symbol
     * @return Flux of tick data updates
     */
    Flux<TickData> streamMarketData(String symbol);

    /**
     * Get options chain for a symbol.
     * Client Portal API: GET /iserver/secdef/search
     *
     * @param symbol Underlying symbol
     * @return Mono with options chain
     */
    Mono<OptionsChain> getOptionsChain(String symbol);

    /**
     * Place an order.
     * Client Portal API: POST /iserver/account/{accountId}/orders
     *
     * @param order Order details
     * @return Mono with order confirmation
     */
    Mono<Map<String, Object>> placeOrder(Order order);

    /**
     * Get order status.
     *
     * @param orderId Order ID
     * @return Mono with order status
     */
    Mono<Map<String, Object>> getOrderStatus(String orderId);

    /**
     * Cancel an order.
     *
     * @param orderId Order ID
     * @return Mono completing when cancelled
     */
    Mono<Void> cancelOrder(String orderId);

    /**
     * Logout and invalidate session.
     *
     * @return Mono completing when logged out
     */
    Mono<Void> logout();
}
