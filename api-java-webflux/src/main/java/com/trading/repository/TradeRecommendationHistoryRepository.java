package com.trading.repository;

import com.trading.repository.model.TradeRecommendationHistory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRecommendationHistoryRepository extends ReactiveMongoRepository<TradeRecommendationHistory,String> {
}
