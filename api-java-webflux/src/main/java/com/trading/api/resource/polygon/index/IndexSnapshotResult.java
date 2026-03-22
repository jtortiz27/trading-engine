package com.trading.api.resource.polygon.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trading.api.resource.polygon.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexSnapshotResult {
    private Double value;
    @JsonProperty("last_updated")
    private Long lastUpdated;
    private TimeFrame timeFrame;
    private String name;
    private String ticker;
    @JsonProperty("market_status")
    private String marketStatus;
    private String type;
    private Session session;
}
