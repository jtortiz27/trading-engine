package com.ib.client;

/**
 * Stub implementation of HistoricalTickLast from IBKR API.
 */
public class HistoricalTickLast {
    private long time;
    private int tickAttribLast;
    private long price;
    private Decimal size;
    private String exchange;
    private String specialConditions;

    public long time() { return time; }
    public void time(long time) { this.time = time; }
    public int tickAttribLast() { return tickAttribLast; }
    public void tickAttribLast(int tickAttribLast) { this.tickAttribLast = tickAttribLast; }
    public long price() { return price; }
    public void price(long price) { this.price = price; }
    public Decimal size() { return size; }
    public void size(Decimal size) { this.size = size; }
    public String exchange() { return exchange; }
    public void exchange(String exchange) { this.exchange = exchange; }
    public String specialConditions() { return specialConditions; }
    public void specialConditions(String specialConditions) { this.specialConditions = specialConditions; }
}
