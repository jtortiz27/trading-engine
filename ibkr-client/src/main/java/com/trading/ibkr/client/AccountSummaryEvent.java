package com.trading.ibkr.client;

/**
 * Represents an account summary event from IBKR.
 */
public class AccountSummaryEvent {

    private final int requestId;
    private final String account;
    private final String tag;
    private final String value;
    private final String currency;
    private final boolean isComplete;

    private AccountSummaryEvent(Builder builder) {
        this.requestId = builder.requestId;
        this.account = builder.account;
        this.tag = builder.tag;
        this.value = builder.value;
        this.currency = builder.currency;
        this.isComplete = builder.isComplete;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int requestId;
        private String account;
        private String tag;
        private String value;
        private String currency;
        private boolean isComplete;

        public Builder requestId(int requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder account(String account) {
            this.account = account;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder isComplete(boolean isComplete) {
            this.isComplete = isComplete;
            return this;
        }

        public AccountSummaryEvent build() {
            return new AccountSummaryEvent(this);
        }
    }

    public int getRequestId() { return requestId; }
    public String getAccount() { return account; }
    public String getTag() { return tag; }
    public String getValue() { return value; }
    public String getCurrency() { return currency; }
    public boolean isComplete() { return isComplete; }

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
