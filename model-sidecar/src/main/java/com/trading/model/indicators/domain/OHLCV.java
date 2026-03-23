package com.trading.model.indicators.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OHLCV (Open, High, Low, Close, Volume) data point.
 * This is the standard input for all indicator calculations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OHLCV {
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
    private Instant timestamp;

    /**
     * Returns true if any of the required OHLCV values are null.
     * Used for basic validation before calculations.
     */
    public boolean isNull() {
        return open == null || high == null || low == null || close == null || volume == null;
    }
}
