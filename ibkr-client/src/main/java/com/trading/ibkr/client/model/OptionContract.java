package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an option contract in an options chain.
 */
@Value
@Builder
@Jacksonized
public class OptionContract {
    
    /**
     * Contract ID
     */
    Long conId;
    
    /**
     * Underlying symbol
     */
    String underlyingSymbol;
    
    /**
     * Option type (CALL or PUT)
     */
    OptionType optionType;
    
    /**
     * Strike price
     */
    BigDecimal strike;
    
    /**
     * Expiration date
     */
    LocalDate expirationDate;
    
    /**
     * Exchange
     */
    String exchange;
    
    /**
     * Trading class
     */
    String tradingClass;
    
    /**
     * Bid price
     */
    BigDecimal bid;
    
    /**
     * Ask price
     */
    BigDecimal ask;
    
    /**
     * Last traded price
     */
    BigDecimal lastPrice;
    
    /**
     * Trading volume
     */
    Long volume;
    
    /**
     * Open interest
     */
    Long openInterest;
    
    /**
     * Implied volatility
     */
    BigDecimal impliedVolatility;
    
    /**
     * Delta
     */
    BigDecimal delta;
    
    /**
     * Gamma
     */
    BigDecimal gamma;
    
    /**
     * Theta
     */
    BigDecimal theta;
    
    /**
     * Vega
     */
    BigDecimal vega;
    
    /**
     * Rho
     */
    BigDecimal rho;
    
    /**
     * Option type enum
     */
    public enum OptionType {
        CALL,
        PUT
    }
}