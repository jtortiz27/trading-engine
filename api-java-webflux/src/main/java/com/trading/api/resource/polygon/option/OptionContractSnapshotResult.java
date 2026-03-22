package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionContractSnapshotResult {
    @JsonProperty("break_even_price")
    private Double breakEvenPrice;
    private OptionContractDailyPerformance day;
    private OptionContractDetails details;
    @JsonProperty("fmv")
    private Double fairMarketValue;
    private Greeks greeks;
    @JsonProperty("implied_volatility")
    private Double impliedVolatility;
    @JsonProperty("last_quote")
    private Quote lastQuoteForContract;
    @JsonProperty("last_trade")
    private Trade lastTrade;
    @JsonProperty("open_interest")
    private Integer openInterest;
    @JsonProperty("underlying_asset")
    private UnderlyingAsset underlyingAsset;
}
