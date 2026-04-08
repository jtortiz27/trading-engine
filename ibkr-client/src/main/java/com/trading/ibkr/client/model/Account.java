package com.trading.ibkr.client.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents an IBKR account.
 */
@Value
@Builder
@Jacksonized
public class Account {
    
    /**
     * Account ID (e.g., "DU123456")
     */
    String accountId;
    
    /**
     * Account alias/nickname
     */
    String accountAlias;
    
    /**
     * Account title/description
     */
    String accountTitle;
    
    /**
     * Account type (e.g., "INDIVIDUAL", "JOINT")
     */
    String accountType;
    
    /**
     * Account status
     */
    String accountStatus;
    
    /**
     * Currency (e.g., "USD")
     */
    String currency;
    
    /**
     * Whether this is a master account (for advisor accounts)
     */
    Boolean isMaster;
    
    /**
     * Whether account is blocked
     */
    Boolean isBlocked;
}