package com.ib.client;

/**
 * Stub implementation of DeltaNeutralContract from IBKR API.
 */
public class DeltaNeutralContract {
    private int conId;
    private double delta;
    private double price;

    public int conId() { return conId; }
    public void conId(int conId) { this.conId = conId; }
    public double delta() { return delta; }
    public void delta(double delta) { this.delta = delta; }
    public double price() { return price; }
    public void price(double price) { this.price = price; }
}
