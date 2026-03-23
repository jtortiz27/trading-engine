package com.trading.api.resource.marketdata;

import java.time.LocalDate;

/**
 * @deprecated Use {@link com.trading.model.marketdata.OptionContract} instead.
 * This class is preserved for backwards compatibility.
 */
@Deprecated
public class OptionContract extends com.trading.model.marketdata.OptionContract {
    public enum OptionType {
        CALL,
        PUT
    }

    public static OptionContract mock(String underlying, Double strike, OptionType type, int expiryDays) {
        return com.trading.model.marketdata.OptionContract.mock(
            underlying, strike, 
            com.trading.model.marketdata.OptionContract.OptionType.valueOf(type.name()), 
            expiryDays);
    }
}
