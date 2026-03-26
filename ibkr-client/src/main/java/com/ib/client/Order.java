package com.ib.client;

/**
 * Stub class of IBKR Order for compilation.
 * This is a placeholder - actual implementation requires TWS API JAR.
 */
public class Order {
    public static final String BUY = "BUY";
    public static final String SELL = "SELL";
    
    public int orderId;
    public int clientId;
    public int permId;
    public String action;
    public double totalQuantity;
    public String orderType;
    public double lmtPrice;
    public double auxPrice;
    public String tif;
    public String account;
    public String clearingAccount;
    public String clearingIntent;
    
    public Order() {
    }
}
