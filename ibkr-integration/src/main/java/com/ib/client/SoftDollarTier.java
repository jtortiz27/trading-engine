package com.ib.client;

/**
 * Represents a soft dollar tier.
 */
public class SoftDollarTier {
    private String name;
    private String value;
    private String displayName;

    public SoftDollarTier() {
    }

    public SoftDollarTier(String name, String value, String displayName) {
        this.name = name;
        this.value = value;
        this.displayName = displayName;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}