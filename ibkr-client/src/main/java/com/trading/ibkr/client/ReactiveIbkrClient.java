package com.trading.ibkr.client;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive interface for IBKR connection management.
 * Provides reactive streams for connection status and lifecycle management.
 */
public interface ReactiveIbkrClient {
    
    /**
     * Connect to IBKR Gateway asynchronously.
     * @return Mono that completes when connection is established
     */
    Mono<Void> connect();
    
    /**
     * Disconnect from IBKR Gateway.
     * @return Mono that completes when disconnected
     */
    Mono<Void> disconnect();
    
    /**
     * Check if currently connected.
     * @return Mono with connection status
     */
    Mono<Boolean> isConnected();
    
    /**
     * Stream of connection status changes.
     * Emits true when connected, false when disconnected.
     */
    Flux<Boolean> connectionStatus();
    
    /**
     * Get the next valid order ID.
     * @return Mono with next order ID
     */
    Mono<Integer> getNextValidId();
    
    /**
     * Request market data for a contract.
     * @param tickerId Unique ticker ID
     * @param contract The contract to subscribe to
     * @return Flux of tick data
     */
    Flux<TickEvent> requestMarketData(int tickerId, Contract contract);
    
    /**
     * Cancel market data subscription.
     * @param tickerId The ticker ID to cancel
     * @return Mono that completes when cancelled
     */
    Mono<Void> cancelMarketData(int tickerId);
    
    /**
     * Request options chain for underlying.
     * @param underlyingConId Underlying contract ID
     * @param underlyingSymbol Underlying symbol (e.g., "AAPL")
     * @return Flux of options chain updates
     */
    Flux<OptionsChainEvent> requestOptionsChain(int underlyingConId, String underlyingSymbol);
    
    /**
     * Place an order.
     * @param orderId Order ID
     * @param contract Contract to trade
     * @param order Order details
     * @return Flux of order status updates
     */
    Flux<OrderStatusEvent> placeOrder(int orderId, Contract contract, Order order);
    
    /**
     * Cancel an order.
     * @param orderId Order ID to cancel
     * @return Mono that completes when cancelled
     */
    Mono<Void> cancelOrder(int orderId);
    
    /**
     * Request contract details.
     * @param reqId Request ID
     * @param contract Contract to look up
     * @return Flux of contract details
     */
    Flux<ContractDetailsEvent> requestContractDetails(int reqId, Contract contract);
    
    /**
     * Request account summary.
     * @param reqId Request ID
     * @param groupName Account group (usually "All")
     * @param tags Comma-separated list of tags to request
     * @return Flux of account summary data
     */
    Flux<AccountSummaryEvent> requestAccountSummary(int reqId, String groupName, String tags);
}
