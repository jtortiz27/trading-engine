package com.trading.api.resource.ticker.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CondorScore {
    private CondorStrategy strategy;
    private Double score;
    private Double driftEff;
}
