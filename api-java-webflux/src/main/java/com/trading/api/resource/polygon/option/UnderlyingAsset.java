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
public class UnderlyingAsset {

    @JsonProperty("change_to_break_even")
    private Double changeToBreakEven;
    @JsonProperty("last_updated")
    private BigInteger lastUpdated;
    private Double price;
    private String ticker;
    private TimeFrame timeFrame;
}
