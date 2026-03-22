package com.trading.ibkr.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionsChain {
  private String underlying;
  private Double underlyingPrice;
  private List<OptionContract> calls;
  private List<OptionContract> puts;
}
