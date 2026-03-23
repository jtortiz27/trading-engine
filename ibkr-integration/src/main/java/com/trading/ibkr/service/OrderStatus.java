package com.trading.ibkr.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Status of an order placed through IBKR Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus {
    private int orderId;
    private String symbol;
    private String status; // Submitted, Filled, Cancelled, etc.
    private long filled;
    private long remaining;
    private double avgFillPrice;
    private Instant timestamp;

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
