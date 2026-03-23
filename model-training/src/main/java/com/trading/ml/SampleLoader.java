package com.trading.ml;

import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import java.time.Instant;
import java.util.List;

public class SampleLoader {

  public static List<StockFeatures> loadFeatures() {
    return List.of(
        createStockFeatures("AAPL", 1.5, "positive", 0.95),
        createStockFeatures("AAPL", -2.0, "negative", 0.90));
  }

  private static StockFeatures createStockFeatures(String symbol, double priceChange, String sentiment, Double confidence) {
    StockFeatures features = new StockFeatures();
    features.setSymbol(symbol);
    features.setPriceChange1Min(priceChange);
    features.setSentimentScore(confidence);
    features.setTimestamp(Instant.now());
    return features;
  }

  public static List<TradeRecommendation> loadLabels() {
    return List.of(
        new TradeRecommendation("AAPL", "BUY", 0.95),
        new TradeRecommendation("AAPL", "SELL", 0.90));
  }
}
