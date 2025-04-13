package com.trading.service;

import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import com.trading.util.LiveMarketFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final WebClient webClient = WebClient.create(System.getenv("MODEL_SERVER_URL"));
    private final LiveMarketFetcher liveMarketFetcher;

    public Mono<TradeRecommendation> generateRecommendation(String symbol) {
        StockFeatures features = liveMarketFetcher.fetch(symbol);
        return webClient.post()
            .uri("/infer")
            .bodyValue(features)
            .retrieve()
            .bodyToMono(TradeRecommendation.class);
    }
}
