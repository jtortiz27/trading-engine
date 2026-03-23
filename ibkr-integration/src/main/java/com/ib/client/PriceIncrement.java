package com.ib.client;

/**
 * Stub implementation of PriceIncrement from IBKR API.
 */
public class PriceIncrement {
    private double lowEdge;
    private double increment;

    public double lowEdge() { return lowEdge; }
    public void lowEdge(double lowEdge) { this.lowEdge = lowEdge; }
    public double increment() { return increment; }
    public void increment(double increment) { this.increment = increment; }
}
