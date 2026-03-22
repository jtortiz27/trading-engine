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
public class TickData {
  private String symbol;
  private Double lastPrice;
  private Double bidPrice;
  private Double askPrice;
  private Double bidSize;
  private Double askSize;
  private Long volume;
  private Double highPrice;
  private Double lowPrice;
  private Double openPrice;
  private Double closePrice;
  private Instant timestamp;
}
