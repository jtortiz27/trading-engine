package com.trading.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockFeatures {
  private double priceChange;
  private String newsSentiment;
  private LocalDateTime timestamp;
  private String symbol;
}
