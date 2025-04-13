package com.trading.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockFeatures {
    private double priceChange;
    private String newsSentiment;
    private LocalDateTime timestamp;
    private String symbol;
}
