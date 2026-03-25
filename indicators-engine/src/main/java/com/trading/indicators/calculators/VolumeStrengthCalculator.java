package com.trading.indicators.calculators;

import com.trading.indicators.model.Bar;
import com.trading.indicators.model.VolumeStrength;
import java.util.List;

/**
 * Volume Strength Calculator.
 *
 * ThinkScript port from Ultra Strategy Dashboard:
 * ```
 * # VolumeStrength
 * def volumeAvg = Average(volume, length);
 * def vr = volume / volumeAvg;
 * def projectedVolume = volume * (390 / BarNumber());
 * ```
 */
public class VolumeStrengthCalculator implements IndicatorCalculator<List<Bar>, VolumeStrength> {

  private final int length;
  private final int totalRthBars;
  private final double strongVolumeThreshold;

  public VolumeStrengthCalculator() {
    this(20, 390, 1.5);
  }

  /**
   * @param length Lookback period for average volume
   * @param totalRthBars Total regular trading hours bars (e.g., 390 for 1-min bars)
   * @param strongVolumeThreshold Threshold for "strong" volume (default 1.5)
   */
  public VolumeStrengthCalculator(int length, int totalRthBars, double strongVolumeThreshold) {
    this.length = length;
    this.totalRthBars = totalRthBars;
    this.strongVolumeThreshold = strongVolumeThreshold;
  }

  @Override
  public VolumeStrength calculate(List<Bar> bars) {
    if (bars == null || bars.size() < length) {
      return VolumeStrength.builder()
          .volumeRatio(1.0)
          .projectedRthVolume(0.0)
          .volumePercentOfAvg(100.0)
          .isStrongVolume(false)
          .build();
    }

    // Get current volume (last bar)
    Bar currentBar = bars.get(bars.size() - 1);
    double currentVolume = currentBar.getVolume();

    // Calculate average volume over the lookback period
    double volumeSum = 0.0;
    int count = 0;
    for (int i = bars.size() - length; i < bars.size(); i++) {
      if (i >= 0) {
        volumeSum += bars.get(i).getVolume();
        count++;
      }
    }

    double avgVolume = count > 0 ? volumeSum / count : currentVolume;

    // Volume Ratio (VR) calculation
    double volumeRatio = avgVolume > 0 ? currentVolume / avgVolume : 1.0;

    // RTH Projection: current volume scaled to full day
    // ThinkScript: volume * (totalBars / currentBarNumber)
    int currentBarNumber = bars.size() % totalRthBars;
    if (currentBarNumber == 0) currentBarNumber = totalRthBars;

    double projectedRthVolume = currentVolume * (totalRthBars / (double) currentBarNumber);

    return VolumeStrength.builder()
        .volumeRatio(volumeRatio)
        .projectedRthVolume(projectedRthVolume)
        .volumePercentOfAvg(volumeRatio * 100.0)
        .isStrongVolume(volumeRatio >= strongVolumeThreshold)
        .build();
  }

  @Override
  public int getRequiredDataPoints() {
    return length;
  }
}
