package com.ib.client;

/**
 * Stub implementation of HistogramEntry from IBKR API.
 */
public class HistogramEntry {
    private double price;
    private long size;

    public double price() { return price; }
    public void price(double price) { this.price = price; }
    public long size() { return size; }
    public void size(long size) { this.size = size; }
}
