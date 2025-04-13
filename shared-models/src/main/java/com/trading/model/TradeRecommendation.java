package com.trading.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TradeRecommendation {
    private String symbol;
    private String action;
    private double confidence;
    // getters/setters
}
