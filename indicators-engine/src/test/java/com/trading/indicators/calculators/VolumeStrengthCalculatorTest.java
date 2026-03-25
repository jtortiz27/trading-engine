package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.VolumeStrength;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeStrengthCalculatorTest {

  @Test
  void shouldCalculateVolumeRatio() {
    VolumeStrengthCalculator calculator = new VolumeStrengthCalculator(5, 390, 1.5);

    List<Bar> bars = createBarsWithVolume(
        new double[]{100, 100, 100, 100, 100, 200});

    VolumeStrength result = calculator.calculate(bars);

    // Volume ratio should be 200 / 100 = 2.0
    assertThat(result.getVolumeRatio()).isEqualTo(2.0);
    assertThat(result.isStrongVolume()).isTrue();
  }

  @Test
  void shouldHandleLowVolume() {
    VolumeStrengthCalculator calculator = new VolumeStrengthCalculator(5, 390, 1.5);

    List<Bar> bars = createBarsWithVolume(
        new double[]{100, 100, 100, 100, 100, 50});

    VolumeStrength result = calculator.calculate(bars);

    // Volume ratio should be 50 / 100 = 0.5
    assertThat(result.getVolumeRatio()).isEqualTo(0.5);
    assertThat(result.isStrongVolume()).isFalse();
  }

  @Test
  void shouldReturnDefaultForInsufficientData() {
    VolumeStrengthCalculator calculator = new VolumeStrengthCalculator(10, 390, 1.5);

    List<Bar> bars = createBarsWithVolume(new double[]{100, 100});

    VolumeStrength result = calculator.calculate(bars);

    assertThat(result.getVolumeRatio()).isEqualTo(1.0);
    assertThat(result.getVolumePercentOfAvg()).isEqualTo(100.0);
  }

  @Test
  void shouldCalculateRthProjection() {
    VolumeStrengthCalculator calculator = new VolumeStrengthCalculator(5, 390, 1.5);

    List<Bar> bars = createBarsWithVolume(
        new double[]{100, 100, 100, 100, 100, 100});

    VolumeStrength result = calculator.calculate(bars);

    // Projection should be current volume * (390 / currentBar)
    assertThat(result.getProjectedRthVolume()).isGreaterThan(0);
  }

  private List<Bar> createBarsWithVolume(double[] volumes) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();

    for (int i = 0; i < volumes.length; i++) {
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(100.0)
          .high(101.0)
          .low(99.0)
          .close(100.0)
          .volume(volumes[i])
          .build());
    }

    return bars;
  }
}
