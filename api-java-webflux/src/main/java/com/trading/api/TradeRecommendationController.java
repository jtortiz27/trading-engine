package com.trading.api;

import com.trading.model.TradeRecommendation;
import com.trading.service.TradeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/recommendations")
public class TradeRecommendationController {
    private final TradeService tradeService;

    public TradeRecommendationController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public Mono<TradeRecommendation> getRecommendation(@RequestParam String symbol) {
        return tradeService.generateRecommendation(symbol);
    }
}
