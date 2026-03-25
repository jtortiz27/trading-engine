package com.trading.indicators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Indicators Engine Application.
 * Provides indicator calculations ported from ThinkScript Ultra Strategy Dashboard v0.5.
 */
@SpringBootApplication
public class IndicatorsEngineApplication {
  public static void main(String[] args) {
    SpringApplication.run(IndicatorsEngineApplication.class, args);
  }
}
