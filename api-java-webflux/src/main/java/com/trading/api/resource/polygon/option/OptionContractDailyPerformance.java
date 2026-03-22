package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionContractDailyPerformance {
    private Double change;
    @JsonProperty("change_percent")
    private Double changePercent;
    private Double close;
    private Double high;
    @JsonProperty("last_updated")
    private BigInteger lastUpdated;
    private Double open;
    @JsonProperty("previous_close")
    private Double previousClose;
    private Integer volume;
    private double vwap;
}
