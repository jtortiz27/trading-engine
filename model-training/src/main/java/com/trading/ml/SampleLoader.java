package com.trading.ml;

import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;

import java.util.List;

public class SampleLoader {

    public static List<StockFeatures> loadFeatures() {
        return List.of(
                new StockFeatures(1.5, "positive", null,"AAPL"),
                new StockFeatures(-2.0, "negative",null,"AAPL")
        );
    }

    public static List<TradeRecommendation> loadLabels() {
        return List.of(
                new TradeRecommendation("AAPL", "BUY", 0.95),
                new TradeRecommendation("AAPL", "SELL", 0.90)
        );
    }
}
