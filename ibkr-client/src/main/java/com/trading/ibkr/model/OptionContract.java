package com.trading.ibkr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionContract {
  private String symbol;
  private String underlying;
  private String expiry; // ISO-8601 date (YYYY-MM-DD)
  private Double strike;
  private OptionType type;
  private Double lastPrice;
  private Double bidPrice;
  private Double askPrice;
  private Long volume;
  private Integer openInterest;
  private Double impliedVolatility;
  private Double delta;
  private Double gamma;
  private Double theta;
  private Double vega;

  public enum OptionType {
    CALL,
    PUT
  }
}
