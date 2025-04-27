package com.trading.util;

import com.trading.api.resource.polygon.ExponentialMovingAverageResource;
import com.trading.model.StockFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class PolygonToStockFeatureMapper {

    public List<StockFeatures> fromEmaResource(ExponentialMovingAverageResource resource) {
        return resource.getUnderlying().getAggregates().stream()
                .map(value -> {
                    StockFeatures features = new StockFeatures();
                    features.setTimestamp(Instant.ofEpochMilli(value.getTimestampForAggregateWindowStart()));
//                    features.setPriceChange(value.getValue()); // or derive change over time
                    features.setSentimentScore(mapSentiment(getNewsSentiment("news"))); // enrich later
                    features.setSymbol(resource.getUnderlying().getAggregates().getFirst().getTicker());
                    return features;
                })
                .toList();
    }

    public static String getNewsSentiment(String news) {
        return "";
    }

    public static Double mapSentiment(String sentiment) {
        return switch (sentiment.toLowerCase()) {
            case "positive" -> 0.75;
            case "neutral"  -> 0.0;
            case "negative" -> -0.75;
            default         -> null;
        };
    }
}
