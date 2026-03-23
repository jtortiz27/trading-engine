package com.ib.client;

/**
 * Stub implementation of Bar from IBKR API (for historical data).
 */
public class Bar {
    private long time;
    private double high;
    private double low;
    private double open;
    private double close;
    private Decimal volume;
    private Decimal wap;
    private int count;

    public long time() { return time; }
    public void time(long time) { this.time = time; }
    public double high() { return high; }
    public void high(double high) { this.high = high; }
    public double low() { return low; }
    public void low(double low) { this.low = low; }
    public double open() { return open; }
    public void open(double open) { this.open = open; }
    public double close() { return close; }
    public void close(double close) { this.close = close; }
    public Decimal volume() { return volume; }
    public void volume(Decimal volume) { this.volume = volume; }
    public Decimal wap() { return wap; }
    public void wap(Decimal wap) { this.wap = wap; }
    public int count() { return count; }
    public void count(int count) { this.count = count; }
}
