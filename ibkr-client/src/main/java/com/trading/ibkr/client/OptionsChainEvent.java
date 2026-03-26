package com.trading.ibkr.client;

import java.time.LocalDate;
import java.util.Set;

/**
 * Represents an options chain update event from IBKR.
 */
public class OptionsChainEvent {
    
    private final int requestId;
    private final String underlyingSymbol;
    private final int underlyingConId;
    private final String exchange;
    private final String tradingClass;
    private final String multiplier;
    private final Set<String> expirations;
    private final Set<Double> strikes;
    private final boolean isComplete;
    
    private OptionsChainEvent(Builder builder) {
        this.requestId = builder.requestId;
        this.underlyingSymbol = builder.underlyingSymbol;
        this.underlyingConId = builder.underlyingConId;
        this.exchange = builder.exchange;
        this.tradingClass = builder.tradingClass;
        this.multiplier = builder.multiplier;
        this.expirations = builder.expirations;
        this.strikes = builder.strikes;
        this.isComplete = builder.isComplete;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int requestId;
        private String underlyingSymbol;
        private int underlyingConId;
        private String exchange;
        private String tradingClass;
        private String multiplier;
        private Set<String> expirations;
        private Set<Double> strikes;
        private boolean isComplete;
        
        public Builder requestId(int requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder underlyingSymbol(String underlyingSymbol) {
            this.underlyingSymbol = underlyingSymbol;
            return this;
        }
        
        public Builder underlyingConId(int underlyingConId) {
            this.underlyingConId = underlyingConId;
            return this;
        }
        
        public Builder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }
        
        public Builder tradingClass(String tradingClass) {
            this.tradingClass = tradingClass;
            return this;
        }
        
        public Builder multiplier(String multiplier) {
            this.multiplier = multiplier;
            return this;
        }
        
        public Builder expirations(Set<String> expirations) {
            this.expirations = expirations;
            return this;
        }
        
        public Builder strikes(Set<Double> strikes) {
            this.strikes = strikes;
            return this;
        }
        
        public Builder isComplete(boolean isComplete) {
            this.isComplete = isComplete;
            return this;
        }
        
        public OptionsChainEvent build() {
            return new OptionsChainEvent(this);
        }
    }
    
    public int getRequestId() { return requestId; }
    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public int getUnderlyingConId() { return underlyingConId; }
    public String getExchange() { return exchange; }
    public String getTradingClass() { return tradingClass; }
    public String getMultiplier() { return multiplier; }
    public Set<String> getExpirations() { return expirations; }
    public Set<Double> getStrikes() { return strikes; }
    public boolean isComplete() { return isComplete; }
    
    /**
     * Creates an intermediate event with expirations and strikes.
     */
    public static OptionsChainEvent intermediate(int requestId, String underlyingSymbol, int underlyingConId,
                                                  String exchange, String tradingClass, String multiplier,
                                                  Set<String> expirations, Set<Double> strikes) {
        return OptionsChainEvent.builder()
            .requestId(requestId)
            .underlyingSymbol(underlyingSymbol)
            .underlyingConId(underlyingConId)
            .exchange(exchange)
            .tradingClass(tradingClass)
            .multiplier(multiplier)
            .expirations(expirations)
            .strikes(strikes)
            .isComplete(false)
            .build();
    }
    
    /**
     * Creates a completion event.
     */
    public static OptionsChainEvent complete(int requestId) {
        return OptionsChainEvent.builder()
            .requestId(requestId)
            .isComplete(true)
            .build();
    }
}
