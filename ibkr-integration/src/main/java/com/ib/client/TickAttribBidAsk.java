package com.ib.client;

/**
 * Stub implementation of TickAttribBidAsk from IBKR API.
 */
public class TickAttribBidAsk {
    private boolean bidPastLow;
    private boolean askPastHigh;

    public boolean bidPastLow() { return bidPastLow; }
    public void bidPastLow(boolean bidPastLow) { this.bidPastLow = bidPastLow; }
    public boolean askPastHigh() { return askPastHigh; }
    public void askPastHigh(boolean askPastHigh) { this.askPastHigh = askPastHigh; }
}
