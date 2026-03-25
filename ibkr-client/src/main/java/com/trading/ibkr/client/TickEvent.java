package com.trading.ibkr.client;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a market data tick event from IBKR.
 */
@Data
@Builder
public class TickEvent {
    
    private final int tickerId;
    private final TickType tickType;
    private final double value;
    private final String stringValue;
    private final long size;
    private final TickAttributes attributes;
    
    /**
     * Standard tick types as defined by IBKR.
     */
    public enum TickType {
        BID_PRICE,           // 1
        BID_SIZE,            // 0
        ASK_PRICE,           // 2
        ASK_SIZE,            // 3
        LAST_PRICE,          // 4
        LAST_SIZE,           // 5
        HIGH,                // 6
        LOW,                 // 7
        VOLUME,              // 8
        CLOSE_PRICE,         // 9
        BID_OPTION,          // 10
        ASK_OPTION,          // 11
        LAST_OPTION,         // 12
        MODEL_OPTION,        // 13
        OPEN,                // 14
        LOW_13_WEEK,         // 15
        HIGH_13_WEEK,        // 16
        LOW_26_WEEK,         // 17
        HIGH_26_WEEK,        // 18
        LOW_52_WEEK,         // 19
        HIGH_52_WEEK,        // 20
        AV_VOLUME,           // 21
        OPEN_INTEREST,       // 22
        OPTION_HISTORICAL_VOL, // 23
        OPTION_IMPLIED_VOL,  // 24
        OPTION_BID_EXCH,     // 25
        OPTION_ASK_EXCH,     // 26
        OPTION_CALL_OPEN_INTEREST, // 27
        OPTION_PUT_OPEN_INTEREST,  // 28
        OPTION_CALL_VOLUME,  // 29
        OPTION_PUT_VOLUME,   // 30
        INDEX_FUTURE_PREMIUM, // 31
        BID_EXCH,            // 32
        ASK_EXCH,            // 33
        AUCTION_VOLUME,      // 34
        AUCTION_PRICE,       // 35
        AUCTION_IMBALANCE,   // 36
        MARK_PRICE,          // 37
        BID_EFP_COMPUTATION, // 38
        ASK_EFP_COMPUTATION, // 39
        LAST_EFP_COMPUTATION, // 40
        OPEN_EFP_COMPUTATION, // 41
        HIGH_BID,            // 42
        LOW_ASK,             // 43
        LAST_RTH_TRADE,      // 44
        RT_HISTORICAL_VOL,   // 45
        IB_DIVIDENDS,        // 59
        BOND_FACTOR_MULTIPLIER, // 60
        REGULATORY_IMBALANCE, // 61
        NEWS_TICK,           // 62
        SHORT_TERM_VOLUME_3_MIN, // 63
        SHORT_TERM_VOLUME_5_MIN, // 64
        SHORT_TERM_VOLUME_10_MIN, // 65
        DELAYED_BID,         // 66
        DELAYED_ASK,         // 67
        DELAYED_LAST,        // 68
        DELAYED_BID_SIZE,    // 69
        DELAYED_ASK_SIZE,    // 70
        DELAYED_LAST_SIZE,   // 71
        DELAYED_HIGH,        // 72
        DELAYED_LOW,         // 73
        DELAYED_VOLUME,      // 74
        DELAYED_CLOSE,       // 75
        DELAYED_OPEN,        // 76
        CREDITMAN_MARK_PRICE, // 77
        CREDITMAN_SLOW_MARK_PRICE, // 78
        DELAYED_BID_OPTION,  // 80
        DELAYED_ASK_OPTION,  // 81
        DELAYED_LAST_OPTION, // 82
        DELAYED_MODEL_OPTION, // 83
        LAST_EXCH,           // 84
        LAST_REG_TIME,       // 85
        FUTURES_OPEN_INTEREST, // 86
        AVERAGE_OPT_VOLUME,  // 87
        UNKNOWN
    }
    
    /**
     * Additional tick attributes.
     */
    @Data
    @Builder
    public static class TickAttributes {
        private final boolean canAutoExecute;
        private final boolean pastLimit;
        private final boolean preOpen;
    }
}
