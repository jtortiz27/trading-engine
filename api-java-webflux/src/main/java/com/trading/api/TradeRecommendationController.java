package com.trading.api;

import com.trading.model.TradeRecommendation;
import com.trading.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/recommendations")
public class TradeRecommendationController {
    private final TradeService tradeService;

    @GetMapping
    public Mono<TradeRecommendation> getRecommendation(@RequestParam String symbol) {
        return tradeService.generateRecommendation(symbol);
    }
}
