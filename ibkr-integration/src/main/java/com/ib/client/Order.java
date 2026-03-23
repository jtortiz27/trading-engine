package com.ib.client;

/**
 * Stub implementation of the IBKR Order class.
 */
public class Order {
    public int orderId() { return 0; }
    public void orderId(int orderId) {}
    public String action() { return ""; }
    public void action(String action) {}
    public Decimal totalQuantity() { return Decimal.get(0); }
    public void totalQuantity(Decimal totalQuantity) {}
    public String orderType() { return ""; }
    public void orderType(String orderType) {}
    public double lmtPrice() { return 0; }
    public void lmtPrice(double lmtPrice) {}
}

class Decimal {
    private final long value;
    private final int scale;

    private Decimal(long value, int scale) {
        this.value = value;
        this.scale = scale;
    }

    public static Decimal get(double val) {
        return new Decimal((long)(val * 10000), 4);
    }

    public double doubleValue() {
        return value / Math.pow(10, scale);
    }
}
