package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.DriftEfficiency;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriftEfficiencyCalculatorTest {

  @Test
  void shouldCalculateDriftEfficiency() {
    DriftEfficiencyCalculator calculator = new DriftEfficiencyCalculator(50.0);

    List<Bar> bars = new ArrayList<>();
    bars.add(Bar.builder()
        .timestamp(Instant.now())
        .open(100.0)
        .high(102.0)
        .low(98.0)
        .close(101.0)
        .volume(1000)
        .build());

    DriftEfficiency result = calculator.calculate(bars);

    // Drift = (101 - 100) / 100 = 1%
    assertThat(result.getDrift()).isEqualTo(1.0);
    // Range = 102 - 98 = 4
    assertThat(result.getRange()).isEqualTo(4.0);
    // Efficiency = 1 / 4 = 25%
    assertThat(result.getEfficiency()).isEqualTo(25.0);
    assertThat(result.isPositiveDrift()).isTrue();
  }

  @Test
  void shouldHandleNegativeDrift() {
    DriftEfficiencyCalculator calculator = new DriftEfficiencyCalculator(50.0);

    List<Bar> bars = new ArrayList<>();
    bars.add(Bar.builder()
        .timestamp(Instant.now())
        .open(100.0)
        .high(102.0)
        .low(98.0)
        .close(99.0)
        .volume(1000)
        .build());

    DriftEfficiency result = calculator.calculate(bars);

    assertThat(result.getDrift()).isEqualTo(-1.0);
    assertThat(result.isPositiveDrift()).isFalse();
  }

  @Test
  void shouldDetectHighEfficiency() {
    DriftEfficiencyCalculator calculator = new DriftEfficiencyCalculator(50.0);

    List<Bar> bars = new ArrayList<>();
    bars.add(Bar.builder()
        .timestamp(Instant.now())
        .open(100.0)
        .high(102.0)
        .low(99.0) // Narrow range
        .close(101.0)
        .volume(1000)
        .build());

    DriftEfficiency result = calculator.calculate(bars);

    // Range = 3, Drift = 1, Efficiency = 1/3 = 33%
    assertThat(result.getEfficiency()).isEqualTo(33.33, org.assertj.core.api.Assertions.within(0.1));
  }

  @Test
  void shouldHandleEmptyInput() {
    DriftEfficiencyCalculator calculator = new DriftEfficiencyCalculator(50.0);

    DriftEfficiency result = calculator.calculate(new ArrayList<>());

    assertThat(result.getDrift()).isEqualTo(0.0);
    assertThat(result.getRange()).isEqualTo(0.0);
    assertThat(result.getEfficiency()).isEqualTo(0.0);
  }
}
