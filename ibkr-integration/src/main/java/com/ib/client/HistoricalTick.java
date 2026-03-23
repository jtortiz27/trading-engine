package com.ib.client;

/**
 * Stub implementation of HistoricalTick from IBKR API.
 */
public class HistoricalTick {
    private long time;
    private long price;
    private Decimal size;

    public long time() { return time; }
    public void time(long time) { this.time = time; }
    public long price() { return price; }
    public void price(long price) { this.price = price; }
    public Decimal size() { return size; }
    public void size(Decimal size) { this.size = size; }
}
