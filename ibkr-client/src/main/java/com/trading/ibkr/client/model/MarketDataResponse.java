package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents market data response from IBKR Client Portal API.
 */
@Value
@Builder
@Jacksonized
public class MarketDataResponse {
    
    /**
     * Symbol/ticker
     */
    String symbol;
    
    /**
     * ConID (contract ID)
     */
    Long conId;
    
    /**
     * Company name
     */
    String companyName;
    
    /**
     * Exchange
     */
    String exchange;
    
    /**
     * Last traded price
     */
    BigDecimal lastPrice;
    
    /**
     * Bid price
     */
    BigDecimal bidPrice;
    
    /**
     * Ask price
     */
    BigDecimal askPrice;
    
    /**
     * Bid size
     */
    Long bidSize;
    
    /**
     * Ask size
     */
    Long askSize;
    
    /**
     * Last traded size
     */
    Long lastSize;
    
    /**
     * Today's high
     */
    BigDecimal high;
    
    /**
     * Today's low
     */
    BigDecimal low;
    
    /**
     * Previous day's close
     */
    BigDecimal close;
    
    /**
     * Today's open
     */
    BigDecimal open;
    
    /**
     * Trading volume
     */
    Long volume;
    
    /**
     * 52-week high
     */
    BigDecimal high52Week;
    
    /**
     * 52-week low
     */
    BigDecimal low52Week;
    
    /**
     * Implied volatility (for options)
     */
    BigDecimal impliedVolatility;
    
    /**
     * Historical volatility
     */
    BigDecimal historicalVolatility;
    
    /**
     * P/E ratio
     */
    BigDecimal peRatio;
    
    /**
     * Dividend amount
     */
    BigDecimal dividend;
    
    /**
     * Dividend yield
     */
    BigDecimal dividendYield;
    
    /**
     * Ex-dividend date
     */
    String exDividendDate;
    
    /**
     * Timestamp of the data
     */
    Instant timestamp;
    
    /**
     * Whether the market is open
     */
    Boolean marketOpen;
    
    /**
     * Currency
     */
    String currency;
    
    /**
     * Description of any error
     */
    String error;
}