package com.trading.ibkr.client;

/**
 * Represents an order status event from IBKR.
 */
public class OrderStatusEvent {
    
    private final int orderId;
    private final OrderStatus status;
    private final long filled;
    private final long remaining;
    private final double avgFillPrice;
    private final double lastFillPrice;
    private final String whyHeld;
    private final double marketCapPrice;
    
    private OrderStatusEvent(Builder builder) {
        this.orderId = builder.orderId;
        this.status = builder.status;
        this.filled = builder.filled;
        this.remaining = builder.remaining;
        this.avgFillPrice = builder.avgFillPrice;
        this.lastFillPrice = builder.lastFillPrice;
        this.whyHeld = builder.whyHeld;
        this.marketCapPrice = builder.marketCapPrice;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int orderId;
        private OrderStatus status;
        private long filled;
        private long remaining;
        private double avgFillPrice;
        private double lastFillPrice;
        private String whyHeld;
        private double marketCapPrice;
        
        public Builder orderId(int orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder filled(long filled) {
            this.filled = filled;
            return this;
        }
        
        public Builder remaining(long remaining) {
            this.remaining = remaining;
            return this;
        }
        
        public Builder avgFillPrice(double avgFillPrice) {
            this.avgFillPrice = avgFillPrice;
            return this;
        }
        
        public Builder lastFillPrice(double lastFillPrice) {
            this.lastFillPrice = lastFillPrice;
            return this;
        }
        
        public Builder whyHeld(String whyHeld) {
            this.whyHeld = whyHeld;
            return this;
        }
        
        public Builder marketCapPrice(double marketCapPrice) {
            this.marketCapPrice = marketCapPrice;
            return this;
        }
        
        public OrderStatusEvent build() {
            return new OrderStatusEvent(this);
        }
    }
    
    public int getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public long getFilled() { return filled; }
    public long getRemaining() { return remaining; }
    public double getAvgFillPrice() { return avgFillPrice; }
    public double getLastFillPrice() { return lastFillPrice; }
    public String getWhyHeld() { return whyHeld; }
    public double getMarketCapPrice() { return marketCapPrice; }
    
    /**
     * Order status as defined by IBKR.
     */
    public enum OrderStatus {
        PENDING_SUBMIT,
        PENDING_CANCEL,
        PRE_SUBMITTED,
        SUBMITTED,
        API_PENDING,
        API_CANCELLED,
        CANCELLED,
        FILLED,
        INACTIVE,
        UNKNOWN
    }
}
