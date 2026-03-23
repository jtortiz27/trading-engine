package com.ib.client;

/**
 * Stub implementation of the IBKR Execution class.
 */
public class Execution {
    public int orderId;
    public String execId;
    public String time;
    public String acctNumber;
    public String exchange;
    public String side;
    public int shares;
    public double price;
}

/**
 * Stub implementation of the IBKR CommissionReport class.
 */
class CommissionReport {
    public String execId;
    public double commission;
    public String currency;
    public double realizedPNL;
}
