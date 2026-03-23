package com.ib.client;

/**
 * Stub implementation of the IBKR Order class.
 */
public class Order {
    private int orderId;
    private String action;
    private Decimal totalQuantity;
    private String orderType;
    private double lmtPrice;

    public int orderId() { return orderId; }
    public void orderId(int orderId) { this.orderId = orderId; }
    public String action() { return action; }
    public void action(String action) { this.action = action; }
    public Decimal totalQuantity() { return totalQuantity; }
    public void totalQuantity(Decimal totalQuantity) { this.totalQuantity = totalQuantity; }
    public String orderType() { return orderType; }
    public void orderType(String orderType) { this.orderType = orderType; }
    public double lmtPrice() { return lmtPrice; }
    public void lmtPrice(double lmtPrice) { this.lmtPrice = lmtPrice; }
}
