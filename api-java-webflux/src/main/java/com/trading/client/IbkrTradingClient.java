package com.trading.client;

import com.trading.api.resource.marketdata.MarketData;
import com.trading.api.resource.marketdata.OptionsChain;
import reactor.core.publisher.Mono;

/**
 * Extension of {@link IbkrMarketDataClient} with order placement capability.
 *
 * <p>This interface adds order placement methods to the base market data client.
 * Used by services that need both market data and trading capabilities.
 */
public interface IbkrTradingClient extends IbkrMarketDataClient {

    /**
     * Places an order for the specified symbol.
     *
     * @param orderRequest the order request details
     * @return Mono emitting order status
     */
    Mono<OrderStatus> placeOrder(OrderRequest orderRequest);

    /**
     * Cancels an existing order.
     *
     * @param orderId the order ID to cancel
     * @return Mono emitting true if cancelled, false otherwise
     */
    Mono<Boolean> cancelOrder(int orderId);

    /**
     * Gets order status for a specific order.
     *
     * @param orderId the order ID
     * @return Mono emitting order status
     */
    Mono<OrderStatus> getOrderStatus(int orderId);

    /**
     * Order request DTO.
     */
    class OrderRequest {
        private String symbol;
        private String action; // BUY or SELL
        private double quantity;
        private String orderType; // LMT, MKT, STP
        private double limitPrice;
        private double stopPrice;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
        public String getOrderType() { return orderType; }
        public void setOrderType(String orderType) { this.orderType = orderType; }
        public double getLimitPrice() { return limitPrice; }
        public void setLimitPrice(double limitPrice) { this.limitPrice = limitPrice; }
        public double getStopPrice() { return stopPrice; }
        public void setStopPrice(double stopPrice) { this.stopPrice = stopPrice; }
    }

    /**
     * Order status DTO.
     */
    class OrderStatus {
        private int orderId;
        private String symbol;
        private String status; // Pending, Filled, Cancelled, etc.
        private double filledQuantity;
        private double remainingQuantity;
        private double avgFillPrice;
        private String message;

        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getFilledQuantity() { return filledQuantity; }
        public void setFilledQuantity(double filledQuantity) { this.filledQuantity = filledQuantity; }
        public double getRemainingQuantity() { return remainingQuantity; }
        public void setRemainingQuantity(double remainingQuantity) { this.remainingQuantity = remainingQuantity; }
        public double getAvgFillPrice() { return avgFillPrice; }
        public void setAvgFillPrice(double avgFillPrice) { this.avgFillPrice = avgFillPrice; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
