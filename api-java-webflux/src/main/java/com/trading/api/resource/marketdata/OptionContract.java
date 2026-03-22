package com.trading.api.resource.marketdata;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an individual option contract from IBKR options chain data.
 * Stub implementation for development and testing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionContract {
  private String symbol;
  private LocalDate expiry;
  private Double strike;
  private OptionType type;
  private Double lastPrice;
  private Double bidPrice;
  private Double askPrice;
  private Long volume;
  private Long openInterest;
  private Double impliedVolatility;
  private Double delta;
  private Double gamma;
  private Double theta;
  private Double vega;
  private Double rho;

  public enum OptionType {
    CALL,
    PUT
  }

  /**
   * Factory method to create a mock option contract for testing.
   *
   * @param underlying the underlying stock symbol
   * @param strike the strike price
   * @param type CALL or PUT
   * @param expiryDays days until expiration
   * @return mock OptionContract instance
   */
  public static OptionContract mock(String underlying, Double strike, OptionType type, int expiryDays) {
    double basePrice = underlying.equals("SPY") ? 450.0 : 150.0;
    double iv = 0.15 + Math.random() * 0.25;
    double timeToExpiry = expiryDays / 365.0;

    return OptionContract.builder()
        .symbol(underlying + (expiryDays < 100 ? "0" : "") + expiryDays + type.toString().charAt(0) + String.format("%.0f", strike * 1000))
        .expiry(LocalDate.now().plusDays(expiryDays))
        .strike(strike)
        .type(type)
        .lastPrice(Math.max(0.01, (basePrice - strike) * (type == OptionType.CALL ? 1 : -1) * Math.random() + iv * basePrice * Math.sqrt(timeToExpiry)))
        .bidPrice(Math.max(0.01, (basePrice - strike) * (type == OptionType.CALL ? 1 : -1) * Math.random()))
        .askPrice(Math.max(0.02, (basePrice - strike) * (type == OptionType.CALL ? 1 : -1) * Math.random() + 0.01))
        .volume((long) (Math.random() * 10000))
        .openInterest((long) (Math.random() * 50000))
        .impliedVolatility(iv)
        .delta(type == OptionType.CALL ? 0.5 + Math.random() * 0.4 : -0.5 - Math.random() * 0.4)
        .gamma(0.01 + Math.random() * 0.04)
        .theta(-0.1 - Math.random() * 0.2)
        .vega(0.1 + Math.random() * 0.2)
        .rho(0.01 + Math.random() * 0.05)
        .build();
  }
}
