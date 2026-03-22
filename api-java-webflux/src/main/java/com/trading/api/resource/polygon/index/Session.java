package com.trading.api.resource.polygon.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private Double change;
    @JsonProperty("change_percent")
    private Double changePercent;
    private Double close;
    private Double high;
    private Double low;
    private Double open;
    @JsonProperty("previous_close")
    private Double previousClose;

}
