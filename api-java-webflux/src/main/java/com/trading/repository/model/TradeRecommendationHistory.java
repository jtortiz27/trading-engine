package com.trading.repository.model;

import com.trading.model.TradeRecommendation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * This class is meant to serve as historical data for Trade recommendations at the instant they are made.
 * The Document key should give insight as to when the recommendation was made and is a composite key of <Ticker>-<Timestamp>
 *
 */
@Data
@Document("RecommendationHistory")
@NoArgsConstructor
@AllArgsConstructor
public class TradeRecommendationHistory {
    private TradeRecommendation recommendation;
    private Instant timestamp;
    @Id
    private String recommendationId;

    public TradeRecommendationHistory (TradeRecommendation recommendation) {
        this.recommendation = recommendation;
        this.timestamp = Instant.now();
        this.recommendationId = recommendation.getSymbol() + "-" + timestamp.toString();
    }
}
