package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trading.api.resource.polygon.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {
    private Double ask;
    @JsonProperty("ask_exchange")
    private Integer askExchange;
    @JsonProperty("ask_size")
    private Integer askSize;
    private Double bid;
    @JsonProperty("bid_exchange")
    private Integer bidExchange;
    @JsonProperty("bid_size")
    private Integer bidSize;
    @JsonProperty("last_updated")
    private BigInteger lastUpdated;
    private Double midpoint;
    private TimeFrame timeFrame;
}
