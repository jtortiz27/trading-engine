package com.ib.client;

/**
 * Stub implementation of HistoricalSession from IBKR API.
 */
public class HistoricalSession {
    private String startDateTime;
    private String endDateTime;
    private String refDate;

    public String startDateTime() { return startDateTime; }
    public void startDateTime(String startDateTime) { this.startDateTime = startDateTime; }
    public String endDateTime() { return endDateTime; }
    public void endDateTime(String endDateTime) { this.endDateTime = endDateTime; }
    public String refDate() { return refDate; }
    public void refDate(String refDate) { this.refDate = refDate; }
}
