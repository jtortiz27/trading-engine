package com.trading.api.resource.ticker.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BollingerBand {
    private Integer length;
    private Double deviation;
    private Double upper;
    private Double mid;
    private Double low;
}
