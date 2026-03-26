package com.trading.ibkr.client;

import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Represents an options chain update event from IBKR.
 */
@Value
@Builder
public class OptionsChainEvent {
    
    Integer requestId;
    String underlyingSymbol;
    Integer underlyingConId;
    String exchange;
    String tradingClass;
    String multiplier;
    Set<String> expirations;
    Set<Double> strikes;
    Boolean complete;
    
    /**
     * Creates an intermediate event with expirations and strikes.
     */
    public static OptionsChainEvent intermediate(Integer requestId, String underlyingSymbol, Integer underlyingConId,
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
            .complete(false)
            .build();
    }
    
    /**
     * Creates a completion event.
     */
    public static OptionsChainEvent complete(Integer requestId) {
        return OptionsChainEvent.builder()
            .requestId(requestId)
            .complete(true)
            .build();
    }
}
