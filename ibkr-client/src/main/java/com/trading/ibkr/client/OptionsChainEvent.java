package com.trading.ibkr.client;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

/**
 * Represents an options chain update event from IBKR.
 */
@Data
@Builder
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
