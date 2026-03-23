package com.ib.client;

/**
 * Stub implementation of HistoricalTickBidAsk from IBKR API.
 */
public class HistoricalTickBidAsk {
    private long time;
    private int tickAttribBidAsk;
    private long priceBid;
    private long priceAsk;
    private Decimal sizeBid;
    private Decimal sizeAsk;

    public long time() { return time; }
    public void time(long time) { this.time = time; }
    public int tickAttribBidAsk() { return tickAttribBidAsk; }
    public void tickAttribBidAsk(int tickAttribBidAsk) { this.tickAttribBidAsk = tickAttribBidAsk; }
    public long priceBid() { return priceBid; }
    public void priceBid(long priceBid) { this.priceBid = priceBid; }
    public long priceAsk() { return priceAsk; }
    public void priceAsk(long priceAsk) { this.priceAsk = priceAsk; }
    public Decimal sizeBid() { return sizeBid; }
    public void sizeBid(Decimal sizeBid) { this.sizeBid = sizeBid; }
    public Decimal sizeAsk() { return sizeAsk; }
    public void sizeAsk(Decimal sizeAsk) { this.sizeAsk = sizeAsk; }
}
