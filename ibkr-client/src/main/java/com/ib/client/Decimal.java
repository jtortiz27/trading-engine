package com.ib.client;

/**
 * Stub implementation of IBKR Decimal class for compilation.
 * This is a placeholder - actual implementation requires TWS API JAR.
 */
public class Decimal {
    private final long value;
    
    private Decimal(long value) {
        this.value = value;
    }
    
    public static Decimal get(long value) {
        return new Decimal(value);
    }
    
    public long longValue() {
        return value;
    }
    
    public double doubleValue() {
        return (double) value;
    }
}
