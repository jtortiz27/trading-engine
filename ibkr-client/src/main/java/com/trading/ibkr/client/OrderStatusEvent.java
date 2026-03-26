package com.trading.ibkr.client;

import lombok.Builder;
import lombok.Value;

/**
 * Represents an order status event from IBKR.
 */
@Value
@Builder
public class OrderStatusEvent {
    
    Integer orderId;
    OrderStatus status;
    Long filled;
    Long remaining;
    Double avgFillPrice;
    Double lastFillPrice;
    String whyHeld;
    Double marketCapPrice;
    
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
