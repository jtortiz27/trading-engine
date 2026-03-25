package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.DriftEfficiency;
import java.util.List;

/**
 * Drift Efficiency Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * def drift = (close - open) / open;
 * def range = high - low;
 * def efficiency = AbsValue(drift) / range * 100;
 * ```
 */
public class DriftEfficiencyCalculator implements IndicatorCalculator<List<Bar>, DriftEfficiency> {

  private final double highEfficiencyThreshold;

  public DriftEfficiencyCalculator() {
    this(50.0);
  }

  /**
   * @param highEfficiencyThreshold Threshold for "high efficiency" (default 50%)
   */
  public DriftEfficiencyCalculator(double highEfficiencyThreshold) {
    this.highEfficiencyThreshold = highEfficiencyThreshold;
  }

  @Override
  public DriftEfficiency calculate(List<Bar> bars) {
    if (bars == null || bars.isEmpty()) {
      return DriftEfficiency.builder()
          .drift(0.0)
          .range(0.0)
          .efficiency(0.0)
          .isPositiveDrift(false)
          .isHighEfficiency(false)
          .efficiencyLevel(DriftEfficiency.EfficiencyLevel.VERY_LOW)
          .build();
    }

    Bar currentBar = bars.get(bars.size() - 1);
    double open = currentBar.getOpen();
    double close = currentBar.getClose();
    double high = currentBar.getHigh();
    double low = currentBar.getLow();

    // Calculate drift
    double drift = 0.0;
    if (open > 0) {
      drift = ((close - open) / open) * 100.0;
    }

    // Calculate range
    double range = high - low;

    // Calculate efficiency
    double efficiency = 0.0;
    if (range > 0) {
      efficiency = (Math.abs(drift) / range) * 100.0;
    }

    return DriftEfficiency.builder()
        .drift(drift)
        .range(range)
        .efficiency(efficiency)
        .isPositiveDrift(drift > 0)
        .isHighEfficiency(efficiency >= highEfficiencyThreshold)
        .efficiencyLevel(DriftEfficiency.determineLevel(efficiency))
        .build();
  }

  @Override
  public int getRequiredDataPoints() {
    return 1;
  }
}
