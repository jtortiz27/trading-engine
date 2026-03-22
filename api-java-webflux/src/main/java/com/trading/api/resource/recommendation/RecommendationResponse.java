package com.trading.api.resource.recommendation;

import com.trading.api.resource.marketdata.MarketData;
import com.trading.api.resource.marketdata.OptionsChain;
import com.trading.model.TradeRecommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Enriched response containing ML model recommendation plus market data from IBKR Gateway.
 *
 * <p>This DTO combines:
 * <ul>
 *   <li>ML model prediction (BUY/SELL/HOLD with confidence)</li>
 *   <li>Real-time market data</li>
 *   <li>Options chain data</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
  private String symbol;
  private String recommendation;
  private Double confidence;
  private MarketData marketData;
  private OptionsChain optionsChain;
  private Instant timestamp;

  /**
   * Factory method to build response from model recommendation and market data.
   *
   * @param tradeRecommendation the ML model prediction
   * @param marketData the market data from IBKR
   * @param optionsChain the options chain from IBKR
   * @return enriched RecommendationResponse
   */
  public static RecommendationResponse from(
      TradeRecommendation tradeRecommendation,
      MarketData marketData,
      OptionsChain optionsChain) {
    return RecommendationResponse.builder()
        .symbol(tradeRecommendation.getSymbol())
        .recommendation(tradeRecommendation.getLabel())
        .confidence(tradeRecommendation.getConfidence())
        .marketData(marketData)
        .optionsChain(optionsChain)
        .timestamp(Instant.now())
        .build();
  }
}
