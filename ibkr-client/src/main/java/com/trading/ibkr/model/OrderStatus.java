package com.trading.ibkr.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus {
  private String orderId;
  private String symbol;
  private OrderStatusEnum status;
  private double filledQuantity;
  private double remainingQuantity;
  private Double avgFillPrice;
  private Double lastFillPrice;
  private String errorMessage;
  private Instant timestamp;

  public enum OrderStatusEnum {
    PENDING_SUBMIT,
    SUBMITTED,
    PENDING_CANCEL,
    CANCELLED,
    FILLED,
    PARTIALLY_FILLED,
    INACTIVE,
    ERROR
  }
}
