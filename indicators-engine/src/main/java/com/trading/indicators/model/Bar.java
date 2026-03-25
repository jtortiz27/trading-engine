package com.trading.indicators.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OHLCV Bar - represents a single price bar.
 * ThinkScript equivalent: open, high, low, close, volume
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bar {
  private Instant timestamp;
  private double open;
  private double high;
  private double low;
  private double close;
  private double volume;
  
  /**
   * Range of the bar (high - low).
   * ThinkScript: high - low
   */
  public double getRange() {
    return high - low;
  }
  
  /**
   * True range accounting for gaps.
   * ThinkScript: Max(high - low, AbsValue(high - close[1]), AbsValue(low - close[1]))
   */
  public double getTrueRange(double previousClose) {
    double range1 = high - low;
    double range2 = Math.abs(high - previousClose);
    double range3 = Math.abs(low - previousClose);
    return Math.max(range1, Math.max(range2, range3));
  }
  
  /**
   * Body of the candle (close - open).
   * ThinkScript: close - open
   */
  public double getBody() {
    return close - open;
  }
  
  /**
   * Typical price (HLC/3).
   * ThinkScript: (high + low + close) / 3
   */
  public double getTypicalPrice() {
    return (high + low + close) / 3.0;
  }
}
