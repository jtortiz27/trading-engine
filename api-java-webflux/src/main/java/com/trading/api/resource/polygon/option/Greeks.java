package com.trading.api.resource.polygon.option;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Greeks {
    private Double delta;
    private Double gamma;
    private Double theta;
    private Double vega;
}
