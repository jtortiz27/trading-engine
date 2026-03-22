package com.trading.api.resource.ticker.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VWAP {
    private Double upperBound;
    private Double lowerBound;
}
