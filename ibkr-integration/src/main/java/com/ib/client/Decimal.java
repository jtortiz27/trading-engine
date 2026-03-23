package com.ib.client;

import java.math.BigDecimal;

/**
 * Stub implementation of Decimal from IBKR API.
 */
public class Decimal {
    private final BigDecimal value;

    private Decimal(BigDecimal value) {
        this.value = value;
    }

    public static Decimal get(double val) {
        return new Decimal(BigDecimal.valueOf(val));
    }

    public static Decimal get(long val) {
        return new Decimal(BigDecimal.valueOf(val));
    }

    public static Decimal get(String val) {
        return new Decimal(new BigDecimal(val));
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    public long longValue() {
        return value.longValue();
    }

    public int intValue() {
        return value.intValue();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
