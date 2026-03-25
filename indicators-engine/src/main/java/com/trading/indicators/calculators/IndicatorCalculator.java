package com.trading.indicators.calculators;

import java.util.List;

/**
 * Core interface for all indicator calculators.
 * Follows pure function principles: no side effects, deterministic output.
 *
 * @param <T> Input type (typically List of Bars or price data)
 * @param <R> Output type (indicator-specific result)
 */
public interface IndicatorCalculator<T, R> {

  /**
   * Calculates the indicator value for the given input.
   *
   * @param input Input data (bars, prices, etc.)
   * @return Calculated indicator value
   */
  R calculate(T input);

  /**
   * Returns the minimum number of data points required for calculation.
   *
   * @return Minimum required input size
   */
  int getRequiredDataPoints();
}
