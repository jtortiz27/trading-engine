package com.trading.api.resource.polygon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aggregate {
    @JsonProperty("T")
    private String ticker;
    @JsonProperty("v")
    private Integer tradingVolume;
    @JsonProperty("vw")
    private double volumeWeightedAveragePrice;
    @JsonProperty("o")
    private Double openPrice;
    @JsonProperty("c")
    private Double closePrice;
    @JsonProperty("h")
    private Double highestPrice;
    @JsonProperty("l")
    private Double lowestPrice;
    @JsonProperty("t")
    private Long timestampForAggregateWindowStart;
    @JsonProperty("n")
    private Integer numberOfTransactionsInWindow;
}
