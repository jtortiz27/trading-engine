package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.CondorScore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CondorScoreCalculatorTest {

  @Test
  void shouldCalculateCondorScore() {
    CondorScoreCalculator calculator = new CondorScoreCalculator();

    List<Bar> bars = createCenteredBars(25, 100.0);

    // Low ADX and good IV/HV ratio
    CondorScoreCalculator.CondorInput input =
        new CondorScoreCalculator.CondorInput(bars, 1.5, 15.0);

    CondorScore result = calculator.calculate(input);

    assertThat(result.getScore()).isGreaterThan(0);
    assertThat(result.getProximityComponent()).isGreaterThan(0);
    assertThat(result.getAdxComponent()).isGreaterThan(0);
    assertThat(result.getIvHvComponent()).isGreaterThan(0);
  }

  @Test
  void shouldDetectFavorableCondor() {
    CondorScoreCalculator calculator = new CondorScoreCalculator();

    // Price in middle of range, low ADX, good IV/HV
    List<Bar> bars = createCenteredBars(20, 100.0);

    CondorScoreCalculator.CondorInput input =
        new CondorScoreCalculator.CondorInput(bars, 1.8, 12.0);

    CondorScore result = calculator.calculate(input);

    assertThat(result.getRecommendation()).isIn(
        CondorScore.CondorRecommendation.MODERATE_CONDOR,
        CondorScore.CondorRecommendation.STRONG_CONDOR
    );
  }

  @Test
  void shouldDetectUnfavorableCondor() {
    CondorScoreCalculator calculator = new CondorScoreCalculator();

    // High ADX (trending) is bad for condors
    List<Bar> bars = createCenteredBars(20, 100.0);

    CondorScoreCalculator.CondorInput input =
        new CondorScoreCalculator.CondorInput(bars, 1.0, 40.0);

    CondorScore result = calculator.calculate(input);

    // High ADX reduces score
    assertThat(result.getAdxComponent()).isEqualTo(60.0);
  }

  @Test
  void shouldCalculateProximity() {
    CondorScoreCalculator calculator = new CondorScoreCalculator();

    // Price exactly at middle
    List<Bar> bars = createCenteredBars(20, 100.0);
    // Last bar at middle
    Bar lastBar = Bar.builder()
        .timestamp(Instant.now())
        .open(100.0)
        .high(101.0)
        .low(99.0)
        .close(100.0) // Middle of range
        .volume(1000)
        .build();
    bars.set(bars.size() - 1, lastBar);

    CondorScoreCalculator.CondorInput input =
        new CondorScoreCalculator.CondorInput(bars, 1.0, 20.0);

    CondorScore result = calculator.calculate(input);

    // Proximity should be near 100 when price is at middle
    assertThat(result.getProximityComponent()).isGreaterThan(50);
  }

  private List<Bar> createCenteredBars(int count, double centerPrice) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();

    // Create a range around center
    double range = 5.0;
    
    for (int i = 0; i < count; i++) {
      // Oscillate around center
      double offset = Math.sin(i * 0.5) * range * 0.8;
      double price = centerPrice + offset;
      
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + 1)
          .low(price - 1)
          .close(price)
          .volume(1000)
          .build());
    }

    return bars;
  }
}
