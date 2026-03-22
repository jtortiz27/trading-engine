package com.trading.api.resource.polygon.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAggregateBarsResource {
    private String ticker;
    private Boolean adjusted;
    private Long queryCount;
    @JsonProperty("request_id")
    private String requestId;
    private Long resultsCount;
    private String status;
    private List<AggregateBarsResult> results;
    @JsonProperty("next_url")
    private String nextUrl;
}
