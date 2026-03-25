package com.trading.indicators.calculators;

import com.trading.indicators.model.ATR14;
import com.trading.indicators.model.Bar;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ATR14CalculatorTest {

  @Test
  void shouldCalculateATR() {
    ATR14Calculator calculator = new ATR14Calculator(14);

    List<Bar> bars = createVolatileBars(20);

    ATR14 result = calculator.calculate(bars);

    assertThat(result.getAtr()).isGreaterThan(0);
    assertThat(result.getTrueRange()).isGreaterThan(0);
    assertThat(result.getAtrPercent()).isGreaterThan(0);
  }

  @Test
  void shouldClassifyVolatilityLevel() {
    ATR14Calculator calculator = new ATR14Calculator(14);

    // High volatility bars
    List<Bar> bars = createHighVolatilityBars(20);

    ATR14 result = calculator.calculate(bars);

    assertThat(result.getVolatilityLevel()).isIn(
        ATR14.VolatilityLevel.HIGH,
        ATR14.VolatilityLevel.VERY_HIGH
    );
  }

  @Test
  void shouldReturnDefaultForInsufficientData() {
    ATR14Calculator calculator = new ATR14Calculator(14);

    List<Bar> bars = createVolatileBars(10);

    ATR14 result = calculator.calculate(bars);

    assertThat(result.getAtr()).isEqualTo(0.0);
  }

  private List<Bar> createVolatileBars(int count) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = 100.0;

    for (int i = 0; i < count; i++) {
      double range = 2.0 + (i % 3); // Varying ranges
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + range)
          .low(price - range)
          .close(price + (i % 2 == 0 ? 1.0 : -1.0))
          .volume(1000)
          .build());
      price += (i % 2 == 0 ? 0.5 : -0.5);
    }

    return bars;
  }

  private List<Bar> createHighVolatilityBars(int count) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = 100.0;

    for (int i = 0; i < count; i++) {
      double range = 5.0; // 5% range
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + range)
          .low(price - range)
          .close(price + (i % 2 == 0 ? 2.0 : -2.0))
          .volume(1000)
          .build());
      price += (i % 2 == 0 ? 1.0 : -1.0);
    }

    return bars;
  }
}
