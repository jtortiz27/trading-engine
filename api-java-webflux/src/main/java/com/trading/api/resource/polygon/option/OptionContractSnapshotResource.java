package com.trading.api.resource.polygon.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionContractSnapshotResource {
    private OptionContractSnapshotResult results;
    private String status;
    @JsonProperty("request_id")
    private String requestId;
}
