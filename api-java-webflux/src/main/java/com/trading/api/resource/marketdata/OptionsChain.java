package com.trading.api.resource.marketdata;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * @deprecated Use {@link com.trading.model.marketdata.OptionsChain} instead.
 * This class is preserved for backwards compatibility.
 */
@Deprecated
@NoArgsConstructor
public class OptionsChain extends com.trading.model.marketdata.OptionsChain {
    public static OptionsChain mock(String symbol) {
        com.trading.model.marketdata.OptionsChain parent = com.trading.model.marketdata.OptionsChain.mock(symbol);
        OptionsChain child = new OptionsChain();
        child.setUnderlying(parent.getUnderlying());
        child.setUnderlyingPrice(parent.getUnderlyingPrice());
        child.setCalls(parent.getCalls());
        child.setPuts(parent.getPuts());
        child.setTimestamp(parent.getTimestamp());
        return child;
    }

    public static Mono<OptionsChain> mockMono(String symbol) {
        return Mono.just(mock(symbol));
    }
}
