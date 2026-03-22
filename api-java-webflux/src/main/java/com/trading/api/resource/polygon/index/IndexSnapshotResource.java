package com.trading.api.resource.polygon.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexSnapshotResource {
    private List<IndexSnapshotResult> results;
    private String status;
    @JsonProperty("request_id")
    private String requestId;
}
