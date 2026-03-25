package com.trading.ibkr.client;

import com.ib.client.ContractDetails;
import lombok.Builder;
import lombok.Data;

/**
 * Represents a contract details event from IBKR.
 */
@Data
@Builder
public class ContractDetailsEvent {
    
    private final int requestId;
    private final ContractDetails contractDetails;
    private final boolean isComplete;
    
    /**
     * Creates an intermediate event with contract details.
     */
    public static ContractDetailsEvent details(int requestId, ContractDetails contractDetails) {
        return ContractDetailsEvent.builder()
            .requestId(requestId)
            .contractDetails(contractDetails)
            .isComplete(false)
            .build();
    }
    
    /**
     * Creates a completion event.
     */
    public static ContractDetailsEvent complete(int requestId) {
        return ContractDetailsEvent.builder()
            .requestId(requestId)
            .isComplete(true)
            .build();
    }
}
