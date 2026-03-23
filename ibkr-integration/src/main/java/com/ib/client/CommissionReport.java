package com.ib.client;

/**
 * Stub implementation of CommissionReport from IBKR API.
 */
public class CommissionReport {
    private String execId;
    private double commission;
    private String currency;
    private double realizedPNL;
    private double yield;
    private int yieldRedemptionDate;

    public String execId() { return execId; }
    public void execId(String execId) { this.execId = execId; }
    public double commission() { return commission; }
    public void commission(double commission) { this.commission = commission; }
    public String currency() { return currency; }
    public void currency(String currency) { this.currency = currency; }
    public double realizedPNL() { return realizedPNL; }
    public void realizedPNL(double realizedPNL) { this.realizedPNL = realizedPNL; }
    public double yield() { return yield; }
    public void yield(double yield) { this.yield = yield; }
    public int yieldRedemptionDate() { return yieldRedemptionDate; }
    public void yieldRedemptionDate(int yieldRedemptionDate) { this.yieldRedemptionDate = yieldRedemptionDate; }
}
