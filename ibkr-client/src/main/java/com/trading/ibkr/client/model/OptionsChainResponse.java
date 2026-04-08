package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Represents options chain response from IBKR Client Portal API.
 */
@Value
@Builder
@Jacksonized
public class OptionsChainResponse {
    
    /**
     * Underlying symbol
     */
    String underlyingSymbol;
    
    /**
     * Underlying contract ID
     */
    Long underlyingConId;
    
    /**
     * Exchange
     */
    String exchange;
    
    /**
     * Trading class
     */
    String tradingClass;
    
    /**
     * Multiplier (usually 100 for options)
     */
    String multiplier;
    
    /**
     * Available expiration dates (YYYYMMDD format)
     */
    Set<String> expirations;
    
    /**
     * Available strike prices
     */
    Set<BigDecimal> strikes;
    
    /**
     * List of option contracts
     */
    List<OptionContract> contracts;
    
    /**
     * Whether the response is complete
     */
    Boolean complete;
    
    /**
     * Error message if any
     */
    String error;
}