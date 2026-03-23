package com.trading.model.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Market data DTO representing real-time market information from IBKR Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
  private String symbol;
  private Double lastPrice;
  private Double bidPrice;
  private Double askPrice;
  private Long volume;
  private Double vix;
  private Instant timestamp;

  /**
   * Factory method to create mock market data for testing.
   *
   * @param symbol the stock symbol
   * @return mock MarketData instance
   */
  public static MarketData mock(String symbol) {
    return MarketData.builder()
        .symbol(symbol)
        .lastPrice(150.0 + Math.random() * 50)
        .bidPrice(149.5 + Math.random() * 50)
        .askPrice(150.5 + Math.random() * 50)
        .volume(1000000L + (long) (Math.random() * 5000000))
        .vix(15.0 + Math.random() * 20)
        .timestamp(Instant.now())
        .build();
  }
}
