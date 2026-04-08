package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a portfolio position.
 */
@Value
@Builder
@Jacksonized
public class Position {
    
    /**
     * Account ID
     */
    String accountId;
    
    /**
     * Contract ID
     */
    Long conId;
    
    /**
     * Symbol
     */
    String symbol;
    
    /**
     * Security type (STK, OPT, etc.)
     */
    String secType;
    
    /**
     * Exchange
     */
    String exchange;
    
    /**
     * Currency
     */
    String currency;
    
    /**
     * Company name
     */
    String companyName;
    
    /**
     * Position quantity (positive for long, negative for short)
     */
    BigDecimal position;
    
    /**
     * Market price
     */
    BigDecimal marketPrice;
    
    /**
     * Market value
     */
    BigDecimal marketValue;
    
    /**
     * Average cost
     */
    BigDecimal averageCost;
    
    /**
     * Unrealized P&L
     */
    BigDecimal unrealizedPnl;
    
    /**
     * Realized P&L
     */
    BigDecimal realizedPnl;
    
    /**
     * Underlying symbol (for options)
     */
    String underlyingSymbol;
    
    /**
     * Strike price (for options)
     */
    BigDecimal strike;
    
    /**
     * Option type: CALL or PUT (for options)
     */
    String putCall;
    
    /**
     * Expiration date YYYYMMDD (for options)
     */
    String expiry;
}