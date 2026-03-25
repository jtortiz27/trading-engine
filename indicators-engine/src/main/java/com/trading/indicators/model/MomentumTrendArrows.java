package com.trading.indicators.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Momentum and Trend Arrow indicator.
 * Ported from ThinkScript Ultra Strategy Dashboard v0.5
 *
 * ThinkScript mapping:
 * - MACD: macd() with standard or custom parameters
 * - RSI: rsi(length)
 * - ADX: adx(length)
 * - Trend: composite signal from above
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentumTrendArrows {
  /**
   * MACD value.
   * ThinkScript: MACD(fastLength, slowLength, macdLength)
   */
  private double macd;

  /**
   * MACD Signal line.
   * ThinkScript: MACD().avg
   */
  private double macdSignal;

  /**
   * MACD Histogram (macd - signal).
   */
  private double macdHistogram;

  /**
   * Is MACD bullish (macd > signal).
   */
  private boolean macdBullish;

  /**
   * RSI value (0-100).
   * ThinkScript: RSI(length)
   */
  private double rsi;

  /**
   * RSI interpretation.
   */
  private RsiZone rsiZone;

  /**
   * ADX value.
   * ThinkScript: ADX(length)
   */
  private double adx;

  /**
   * Is trend strong (ADX > 25).
   */
  private boolean isStrongTrend;

  /**
   * DI+ value (bullish directional movement).
   * ThinkScript: DIPlus
   */
  private double diPlus;

  /**
   * DI- value (bearish directional movement).
   * ThinkScript: DIMinus
   */
  private double diMinus;

  /**
   * DI+ > DI- (bullish directional bias).
   */
  private boolean diBullish;

  /**
   * Composite trend direction.
   */
  private TrendDirection trendDirection;

  /**
   * Trend strength classification.
   */
  private TrendStrength trendStrength;

  public enum RsiZone {
    OVERSOLD,    // RSI < 30
    BEARISH,     // 30-45
    NEUTRAL,     // 45-55
    BULLISH,     // 55-70
    OVERBOUGHT   // RSI > 70
  }

  public enum TrendDirection {
    STRONG_UP,
    UP,
    NEUTRAL,
    DOWN,
    STRONG_DOWN
  }

  public enum TrendStrength {
    VERY_WEAK,  // ADX < 10
    WEAK,       // 10-20
    MODERATE,   // 20-30
    STRONG,     // 30-40
    VERY_STRONG // ADX > 40
  }

  /**
   * Determines trend strength from ADX value.
   */
  public static TrendStrength determineStrength(double adx) {
    if (adx < 10) return TrendStrength.VERY_WEAK;
    if (adx < 20) return TrendStrength.WEAK;
    if (adx < 30) return TrendStrength.MODERATE;
    if (adx < 40) return TrendStrength.STRONG;
    return TrendStrength.VERY_STRONG;
  }

  /**
   * Determines RSI zone from value.
   */
  public static RsiZone determineRsiZone(double rsi) {
    if (rsi < 30) return RsiZone.OVERSOLD;
    if (rsi < 45) return RsiZone.BEARISH;
    if (rsi < 55) return RsiZone.NEUTRAL;
    if (rsi < 70) return RsiZone.BULLISH;
    return RsiZone.OVERBOUGHT;
  }
}
