package com.trading.ibkr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
  private String symbol;
  private OrderAction action;
  private OrderType orderType;
  private double quantity;
  private Double limitPrice; // Required for LIMIT orders
  private Double stopPrice; // Required for STOP orders
  private TimeInForce timeInForce;
  private String orderId; // Assigned after submission

  public enum OrderAction {
    BUY,
    SELL
  }

  public enum OrderType {
    MARKET,
    LIMIT,
    STOP,
    STOP_LIMIT
  }

  public enum TimeInForce {
    DAY,
    GTC, // Good Till Canceled
    IOC, // Immediate or Cancel
    FOK // Fill or Kill
  }
}
