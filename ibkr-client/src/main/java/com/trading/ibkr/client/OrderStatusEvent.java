package com.trading.ibkr.client;

import lombok.Builder;
import lombok.Data;

/**
 * Represents an order status event from IBKR.
 */
@Data
@Builder
public class OrderStatusEvent {
    
    private final int orderId;
    private final OrderStatus status;
    private final long filled;
    private final long remaining;
    private final double avgFillPrice;
    private final double lastFillPrice;
    private final String whyHeld;
    private final double marketCapPrice;
    
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
