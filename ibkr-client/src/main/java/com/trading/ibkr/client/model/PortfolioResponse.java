package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents portfolio response from IBKR Client Portal API.
 */
@Value
@Builder
@Jacksonized
public class PortfolioResponse {
    
    /**
     * Account ID
     */
    String accountId;
    
    /**
     * List of positions
     */
    List<Position> positions;
    
    /**
     * Total portfolio value
     */
    BigDecimal totalValue;
    
    /**
     * Total cash balance
     */
    BigDecimal cashBalance;
    
    /**
     * Net liquidation value
     */
    BigDecimal netLiquidation;
    
    /**
     * Unrealized P&L
     */
    BigDecimal unrealizedPnl;
    
    /**
     * Realized P&L
     */
    BigDecimal realizedPnl;
    
    /**
     * Buying power
     */
    BigDecimal buyingPower;
    
    /**
     * Day trades remaining
     */
    Integer dayTradesRemaining;
    
    /**
     * Error message if any
     */
    String error;
}