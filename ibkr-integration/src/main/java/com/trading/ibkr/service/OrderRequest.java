package com.trading.ibkr.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for placing orders through IBKR Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String symbol;
    private String action; // BUY or SELL
    private double quantity;
    private String orderType; // MKT, LMT, STP, etc.
    private double limitPrice;
    private double stopPrice;
    private String timeInForce; // DAY, GTC, etc.
    private boolean outsideRth; // Outside regular trading hours
}
