package com.trading.util;

import com.trading.api.resource.polygon.stock.ExponentialMovingAverageResource;
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
                    features.setSymbol(value.getTicker());
                    features.setCurrentVolume(value.getTradingVolume().doubleValue());
                    return features;
                })
                .toList();
    }

    public static String getNewsSentiment(String news) {
        return "";
    }

    public static Double mapSentiment(String sentiment) {
        if (sentiment == null) {
            return null;
        }
        String lower = sentiment.toLowerCase();
        if ("positive".equals(lower)) {
            return 0.75;
        } else if ("neutral".equals(lower)) {
            return 0.0;
        } else if ("negative".equals(lower)) {
            return -0.75;
        } else {
            return null;
        }
    }
}
