package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.VWAPBands;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VWAPBandsCalculatorTest {

  @Test
  void shouldCalculateVWAP() {
    VWAPBandsCalculator calculator = new VWAPBandsCalculator(1.5, Integer.MAX_VALUE);

    List<Bar> bars = createBarsWithVolume(25);

    VWAPBands result = calculator.calculate(bars);

    assertThat(result.getVwap()).isGreaterThan(0);
    assertThat(result.getUpperBand()).isGreaterThan(result.getVwap());
    assertThat(result.getLowerBand()).isLessThan(result.getVwap());
    assertThat(result.getBandMultiplier()).isEqualTo(1.5);
  }

  @Test
  void shouldDeterminePositionWithinBands() {
    VWAPBandsCalculator calculator = new VWAPBandsCalculator(1.5, Integer.MAX_VALUE);

    List<Bar> bars = createBarsWithVolume(20);

    VWAPBands result = calculator.calculate(bars);

    assertThat(result.getPosition()).isIn(
        VWAPBands.VwapPosition.NEAR_UPPER,
        VWAPBands.VwapPosition.NEAR_LOWER,
        VWAPBands.VwapPosition.AT_VWAP
    );
  }

  @Test
  void shouldDetectAboveVWAP() {
    VWAPBandsCalculator calculator = new VWAPBandsCalculator(1.5, Integer.MAX_VALUE);

    // Create bars with increasing prices
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = 100.0;

    for (int i = 0; i < 19; i++) {
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + 1)
          .low(price - 1)
          .close(price)
          .volume(1000)
          .build());
    }
    // Add a bar with higher close
    bars.add(Bar.builder()
        .timestamp(timestamp.plusSeconds(19 * 60))
        .open(105.0)
        .high(106.0)
        .low(104.0)
        .close(105.0)
        .volume(500)
        .build());

    VWAPBands result = calculator.calculate(bars);

    // VWAP should be below current price
    assertThat(result.getVwap()).isLessThan(105.0);
  }

  private List<Bar> createBarsWithVolume(int count) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = 100.0;

    for (int i = 0; i < count; i++) {
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + 1)
          .low(price - 1)
          .close(price + (i % 3 == 0 ? 0.5 : -0.5))
          .volume(1000 + i * 100) // Varying volume
          .build());
      price += 0.1;
    }

    return bars;
  }
}
