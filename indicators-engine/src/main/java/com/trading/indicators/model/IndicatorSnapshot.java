package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Complete snapshot of all indicators.
 * Aggregates all ThinkScript-ported indicators into a single result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorSnapshot {
  private Instant timestamp;
  private String symbol;

  // Volume indicators
  private VolumeStrength volumeStrength;

  // Volatility indicators
  private IVDashboard ivDashboard;
  private HVPercentile hvPercentile;
  private ATR14 atr14;

  // Correlation
  private CorrelationResult correlation;

  // Options Greeks
  private ATMVega atmVega;

  // Momentum/Trend
  private MomentumTrendArrows momentumTrend;

  // Strategy scores
  private CondorScore condorScore;

  // Price action
  private DriftEfficiency driftEfficiency;

  // Price levels
  private VWAPBands vwapBands;
  private BollingerBands bollingerBands;
}
