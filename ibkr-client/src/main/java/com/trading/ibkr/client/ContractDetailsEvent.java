package com.trading.ibkr.client;

import com.ib.client.ContractDetails;

/**
 * Represents a contract details event from IBKR.
 */
public class ContractDetailsEvent {
    
    private final int requestId;
    private final ContractDetails contractDetails;
    private final boolean isComplete;
    
    private ContractDetailsEvent(Builder builder) {
        this.requestId = builder.requestId;
        this.contractDetails = builder.contractDetails;
        this.isComplete = builder.isComplete;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int requestId;
        private ContractDetails contractDetails;
        private boolean isComplete;
        
        public Builder requestId(int requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder contractDetails(ContractDetails contractDetails) {
            this.contractDetails = contractDetails;
            return this;
        }
        
        public Builder isComplete(boolean isComplete) {
            this.isComplete = isComplete;
            return this;
        }
        
        public ContractDetailsEvent build() {
            return new ContractDetailsEvent(this);
        }
    }
    
    public int getRequestId() { return requestId; }
    public ContractDetails getContractDetails() { return contractDetails; }
    public boolean isComplete() { return isComplete; }
    
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
