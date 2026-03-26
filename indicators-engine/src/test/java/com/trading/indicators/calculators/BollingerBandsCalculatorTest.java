package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.BollingerBands;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BollingerBandsCalculatorTest {

  @Test
  void shouldCalculateBollingerBands() {
    BollingerBandsCalculator calculator = new BollingerBandsCalculator(20, 2.0, 0.10);

    List<Bar> bars = createTrendingBars(25, 100.0, 0.5);

    BollingerBands result = calculator.calculate(bars);

    assertThat(result.getMiddle()).isGreaterThan(0);
    assertThat(result.getUpper()).isGreaterThan(result.getMiddle());
    assertThat(result.getLower()).isLessThan(result.getMiddle());
    assertThat(result.getStandardDeviation()).isGreaterThan(0);
  }

  @Test
  void shouldDetectPriceAboveUpperBand() {
    BollingerBandsCalculator calculator = new BollingerBandsCalculator(20, 2.0, 0.10);

    // Create 20 bars with very low volatility to establish bands around 100
    List<Bar> bars = createFlatBars(20, 100.0, 0.01);
    
    // Add a spike bar that should exceed the established bands
    // At 100 with stdDev ~0.0058, bands are ~100 ± 0.0116, so 115 definitely exceeds
    bars.add(Bar.builder()
        .timestamp(Instant.now())
        .open(115.0)
        .high(115.0)
        .low(115.0)
        .close(115.0)
        .volume(1000)
        .build());

    BollingerBands result = calculator.calculate(bars);

    assertThat(result.isAboveUpper()).isTrue();
  }

  @Test
  void shouldDetectSqueezeCondition() {
    BollingerBandsCalculator calculator = new BollingerBandsCalculator(20, 2.0, 0.15);

    // Create bars with very low volatility
    List<Bar> bars = createFlatBars(25, 100.0, 0.01);

    BollingerBands result = calculator.calculate(bars);

    assertThat(result.isSqueeze()).isTrue();
    assertThat(result.getBandwidthLevel()).isEqualTo(BollingerBands.BandwidthLevel.SQUEEZE);
  }

  @Test
  void shouldReturnDefaultForInsufficientData() {
    BollingerBandsCalculator calculator = new BollingerBandsCalculator(20, 2.0, 0.10);

    List<Bar> bars = createTrendingBars(10, 100.0, 0.5);

    BollingerBands result = calculator.calculate(bars);

    assertThat(result.getMiddle()).isEqualTo(0.0);
    assertThat(result.getPercentB()).isEqualTo(0.5);
  }

  private List<Bar> createTrendingBars(int count, double startPrice, double increment) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = startPrice;

    for (int i = 0; i < count; i++) {
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + 0.5)
          .low(price - 0.5)
          .close(price + increment)
          .volume(1000)
          .build());
      price += increment;
    }

    return bars;
  }

  private List<Bar> createFlatBars(int count, double price, double range) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();

    for (int i = 0; i < count; i++) {
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + range)
          .low(price - range)
          .close(price)
          .volume(1000)
          .build());
    }

    return bars;
  }
}
