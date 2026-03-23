package com.trading.model.marketdata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Options chain DTO representing all available options for an underlying symbol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionsChain {
  private String underlying;
  private Double underlyingPrice;
  private List<OptionContract> calls;
  private List<OptionContract> puts;
  private LocalDate timestamp;

  /**
   * Factory method to create mock options chain data for testing.
   *
   * @param symbol the underlying stock symbol
   * @return mock OptionsChain instance
   */
  public static OptionsChain mock(String symbol) {
    double basePrice = symbol.equals("SPY") ? 450.0 : 150.0;
    List<OptionContract> calls = new ArrayList<>();
    List<OptionContract> puts = new ArrayList<>();

    // Generate strikes around current price
    int[] expiries = {7, 14, 30, 60, 90};
    double[] strikes = {
      basePrice * 0.85,
      basePrice * 0.90,
      basePrice * 0.95,
      basePrice,
      basePrice * 1.05,
      basePrice * 1.10,
      basePrice * 1.15
    };

    for (int expiry : expiries) {
      for (double strike : strikes) {
        calls.add(OptionContract.mock(symbol, strike, OptionContract.OptionType.CALL, expiry));
        puts.add(OptionContract.mock(symbol, strike, OptionContract.OptionType.PUT, expiry));
      }
    }

    return OptionsChain.builder()
        .underlying(symbol)
        .underlyingPrice(basePrice + Math.random() * 10 - 5)
        .calls(calls)
        .puts(puts)
        .timestamp(LocalDate.now())
        .build();
  }
}
