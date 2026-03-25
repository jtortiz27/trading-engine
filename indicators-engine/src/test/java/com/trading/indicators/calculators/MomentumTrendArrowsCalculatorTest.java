package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.MomentumTrendArrows;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MomentumTrendArrowsCalculatorTest {

  @Test
  void shouldCalculateMACD() {
    MomentumTrendArrowsCalculator calculator = new MomentumTrendArrowsCalculator();

    List<Bar> bars = createTrendingBars(50, true); // Uptrend

    MomentumTrendArrows result = calculator.calculate(bars);

    assertThat(result.getMacd()).isNotEqualTo(0);
    assertThat(result.getMacdSignal()).isNotEqualTo(0);
    assertThat(result.getMacdHistogram()).isNotEqualTo(0);
  }

  @Test
  void shouldCalculateRSI() {
    MomentumTrendArrowsCalculator calculator = new MomentumTrendArrowsCalculator();

    List<Bar> bars = createTrendingBars(30, true);

    MomentumTrendArrows result = calculator.calculate(bars);

    assertThat(result.getRsi()).isBetween(0.0, 100.0);
    assertThat(result.getRsiZone()).isNotNull();
  }

  @Test
  void shouldDetectBullishTrend() {
    MomentumTrendArrowsCalculator calculator = new MomentumTrendArrowsCalculator();

    List<Bar> bars = createTrendingBars(50, true); // Strong uptrend

    MomentumTrendArrows result = calculator.calculate(bars);

    assertThat(result.getTrendDirection()).isIn(
        MomentumTrendArrows.TrendDirection.UP,
        MomentumTrendArrows.TrendDirection.STRONG_UP
    );
  }

  @Test
  void shouldDetectBearishTrend() {
    MomentumTrendArrowsCalculator calculator = new MomentumTrendArrowsCalculator();

    List<Bar> bars = createTrendingBars(50, false); // Downtrend

    MomentumTrendArrows result = calculator.calculate(bars);

    assertThat(result.getTrendDirection()).isIn(
        MomentumTrendArrows.TrendDirection.DOWN,
        MomentumTrendArrows.TrendDirection.STRONG_DOWN
    );
  }

  @Test
  void shouldClassifyRSIZones() {
    // Overbought
    List<Bar> overboughtBars = createTrendingBars(30, true);
    MomentumTrendArrowsCalculator calc1 = new MomentumTrendArrowsCalculator();
    MomentumTrendArrows result1 = calc1.calculate(overboughtBars);
    
    if (result1.getRsi() > 70) {
      assertThat(result1.getRsiZone()).isEqualTo(MomentumTrendArrows.RsiZone.OVERBOUGHT);
    }

    // Oversold
    List<Bar> oversoldBars = createTrendingBars(30, false);
    MomentumTrendArrowsCalculator calc2 = new MomentumTrendArrowsCalculator();
    MomentumTrendArrows result2 = calc2.calculate(oversoldBars);
    
    if (result2.getRsi() < 30) {
      assertThat(result2.getRsiZone()).isEqualTo(MomentumTrendArrows.RsiZone.OVERSOLD);
    }
  }

  private List<Bar> createTrendingBars(int count, boolean uptrend) {
    List<Bar> bars = new ArrayList<>();
    Instant timestamp = Instant.now();
    double price = 100.0;
    double direction = uptrend ? 1.0 : -1.0;

    for (int i = 0; i < count; i++) {
      double change = 0.5 * direction;
      double volatility = 0.3;
      
      bars.add(Bar.builder()
          .timestamp(timestamp.plusSeconds(i * 60))
          .open(price)
          .high(price + volatility)
          .low(price - volatility)
          .close(price + change)
          .volume(1000)
          .build());
          
      price += change;
    }

    return bars;
  }
}
