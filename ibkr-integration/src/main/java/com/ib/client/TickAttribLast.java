package com.ib.client;

/**
 * Stub implementation of TickAttribLast from IBKR API.
 */
public class TickAttribLast {
    private boolean pastLimit;
    private boolean unreported;

    public boolean pastLimit() { return pastLimit; }
    public void pastLimit(boolean pastLimit) { this.pastLimit = pastLimit; }
    public boolean unreported() { return unreported; }
    public void unreported(boolean unreported) { this.unreported = unreported; }
}
