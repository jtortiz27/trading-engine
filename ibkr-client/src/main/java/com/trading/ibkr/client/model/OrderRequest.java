package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents an order request to IBKR Client Portal API.
 */
@Value
@Builder
@Jacksonized
public class OrderRequest {
    
    /**
     * Contract ID (required)
     */
    Long conId;
    
    /**
     * Symbol (required if conId not provided)
     */
    String symbol;
    
    /**
     * Security type: STK, OPT, FUT, etc.
     */
    String secType;
    
    /**
     * Exchange (default SMART)
     */
    String exchange;
    
    /**
     * Currency (default USD)
     */
    String currency;
    
    /**
     * Order action: BUY or SELL
     */
    OrderAction action;
    
    /**
     * Total order quantity
     */
    BigDecimal totalQuantity;
    
    /**
     * Order type: LMT, MKT, STP, etc.
     */
    OrderType orderType;
    
    /**
     * Limit price (required for LMT orders)
     */
    BigDecimal lmtPrice;
    
    /**
     * Auxiliary price (for STP orders)
     */
    BigDecimal auxPrice;
    
    /**
     * Time in force: DAY, GTC, IOC, etc.
     */
    TimeInForce tif;
    
    /**
     * Whether to transmit the order
     */
    Boolean transmit;
    
    /**
     * Parent order ID (for bracket orders)
     */
    String parentId;
    
    /**
     * Order reference ID
     */
    String orderRef;
    
    /**
     * Outside RTH (Regular Trading Hours)
     */
    Boolean outsideRth;
    
    /**
     * Order action enum
     */
    public enum OrderAction {
        BUY("BUY"),
        SELL("SELL");
        
        private final String value;
        
        OrderAction(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Order type enum
     */
    public enum OrderType {
        MARKET("MKT"),
        LIMIT("LMT"),
        STOP("STP"),
        STOP_LIMIT("STP LMT"),
        TRAILING_STOP("TRAIL"),
        TRAILING_STOP_LIMIT("TRAIL LIMIT");
        
        private final String value;
        
        OrderType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Time in force enum
     */
    public enum TimeInForce {
        DAY("DAY"),
        GOOD_TILL_CANCEL("GTC"),
        IMMEDIATE_OR_CANCEL("IOC"),
        FILL_OR_KILL("FOK"),
        GOOD_TILL_DATE("GTD");
        
        private final String value;
        
        TimeInForce(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}