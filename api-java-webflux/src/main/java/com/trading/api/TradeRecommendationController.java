package com.trading.api;

import com.trading.model.TradeRecommendation;
import com.trading.service.TradeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/recommendation")
public class TradeRecommendationController {
    private final TradeService tradeService;

    public TradeRecommendationController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping("/{symbol}")
    public Mono<TradeRecommendation> getRecommendation(@PathVariable String symbol) {
        return tradeService.generateRecommendation(symbol);
    }
}
