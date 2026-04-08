package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Represents order response from IBKR Client Portal API.
 */
@Value
@Builder
@Jacksonized
public class OrderResponse {
    
    /**
     * Order ID
     */
    String orderId;
    
    /**
     * Order status
     */
    OrderStatus status;
    
    /**
     * Account ID
     */
    String accountId;
    
    /**
     * Symbol
     */
    String symbol;
    
    /**
     * ConID
     */
    Long conId;
    
    /**
     * Order action
     */
    String action;
    
    /**
     * Total quantity
     */
    BigDecimal totalQuantity;
    
    /**
     * Filled quantity
     */
    BigDecimal filledQuantity;
    
    /**
     * Remaining quantity
     */
    BigDecimal remainingQuantity;
    
    /**
     * Average fill price
     */
    BigDecimal avgFillPrice;
    
    /**
     * Last fill price
     */
    BigDecimal lastFillPrice;
    
    /**
     * Order type
     */
    String orderType;
    
    /**
     * Limit price
     */
    BigDecimal lmtPrice;
    
    /**
     * Auxiliary price (stop price)
     */
    BigDecimal auxPrice;
    
    /**
     * Time in force
     */
    String tif;
    
    /**
     * Order submission time
     */
    Instant orderTime;
    
    /**
     * Order status warnings
     */
    List<String> warnings;
    
    /**
     * Error message if order failed
     */
    String errorMessage;
    
    /**
     * Order status enum
     */
    public enum OrderStatus {
        PENDING_SUBMIT,
        PENDING_CANCEL,
        PRE_SUBMITTED,
        SUBMITTED,
        CANCELLED,
        FILLED,
        INACTIVE,
        API_PENDING,
        API_CANCELLED,
        UNKNOWN
    }
}