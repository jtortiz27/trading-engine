package com.trading.ibkr.client;

import lombok.Builder;
import lombok.Data;

/**
 * Represents an account summary event from IBKR.
 */
@Data
@Builder
public class AccountSummaryEvent {

    private final int requestId;
    private final String account;
    private final String tag;
    private final String value;
    private final String currency;
    private final boolean isComplete;

    /**
     * Creates an event with account summary data.
     */
    public static AccountSummaryEvent data(int requestId, String account, String tag, String value, String currency) {
        return AccountSummaryEvent.builder()
                .requestId(requestId)
                .account(account)
                .tag(tag)
                .value(value)
                .currency(currency)
                .isComplete(false)
                .build();
    }

    /**
     * Creates a completion event.
     */
    public static AccountSummaryEvent complete(int requestId) {
        return AccountSummaryEvent.builder()
                .requestId(requestId)
                .isComplete(true)
                .build();
    }
}
