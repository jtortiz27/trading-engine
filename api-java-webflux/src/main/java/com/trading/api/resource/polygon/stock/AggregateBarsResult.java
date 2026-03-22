package com.trading.api.resource.polygon.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregateBarsResult {
    @JsonProperty("c")
    private Double closePrice;
    @JsonProperty("h")
    private Double highPrice;
    @JsonProperty("l")
    private Double lowPrice;
    @JsonProperty("n")
    private Long numberOfTransactions;
    @JsonProperty("o")
    private Double openPrice;
    @JsonProperty("otc")
    private Boolean OTC;
    @JsonProperty("t")
    private Long timestampForStartOfWindow;
    @JsonProperty("v")
    private Long tradingVolume;
    @JsonProperty("vw")
    private Double weightedVolume;
}
