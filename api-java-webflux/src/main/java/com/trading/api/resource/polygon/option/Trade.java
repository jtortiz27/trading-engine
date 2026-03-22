package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trading.api.resource.polygon.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    private List<Integer> conditions;
    private Integer exchange;
    private Double price;
    @JsonProperty("sip_timestamp")
    private BigInteger timestamp;
    @JsonProperty("size")
    private Integer volume;
    private TimeFrame timeFrame;
}
