package com.trading.api.resource.ticker.summary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TickerSummaryResource {
    private Macro macro;
    private Double volumeStrength;
    private Double impliedVolatility;
    private Double rank;
    private Double impliedVolatilityHistoricalVolatilityRatio;
    private Double impliedVolatilityRealized20Day;
    private Double historicalVolatility30Day;
    private Double sp500Correlation;
    private Double vegaPercent;
    private Trend trend;
    private CondorScore condor;
    private Double atr14;
    private VWAP vwap;
    private BollingerBand bollingerBands;
    private Instant timestamp;
}
