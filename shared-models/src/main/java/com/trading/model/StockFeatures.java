package com.trading.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockFeatures {
    private String symbol;

    // 📈 Trend
    private Double ema5;
    private Double ema10;
    private Double ema20;
    private Double macd;
    private Double macdSignal;

    // 📉 Volatility
    private Double bollingerUpper;
    private Double bollingerLower;
    private Double atr;

    // ⚡ Momentum
    private Double rsi;
    private Double priceChange1Min;
    private Double priceChange5Min;

    // 🔊 Volume
    private Double currentVolume;
    private Double volumeSma10;
    private Double volumeDelta;

    // 🧠 Sentiment
    private Double sentimentScore;
    private Integer headlineCountPast5Min;

    // ⏱️ Time context
    private Integer minutesSinceOpen;
    private Integer minutesUntilClose;
    private Integer dayOfWeek;

    // 🏷️ Label for training
    private String label; // "BUY", "SELL", "HOLD"
    private Instant timestamp;

}
