package com.trading.service;

import com.trading.model.StockFeatures;
import com.trading.model.TradeRecommendation;
import com.trading.util.LiveMarketFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {
  private final WebClient webClient;
  private final LiveMarketFetcher liveMarketFetcher;

  public Mono<TradeRecommendation> generateRecommendation(String symbol) {
    List<StockFeatures> features = liveMarketFetcher.fetch(symbol);
    return webClient
        .post()
        .uri("/infer")
        .bodyValue(features)
        .retrieve()
        .bodyToMono(TradeRecommendation.class);
  }
}
