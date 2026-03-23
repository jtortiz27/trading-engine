package com.trading.ibkr.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * IBKR Gateway connection configuration properties.
 * <p>
 * Configuration is loaded from application.yml under the 'ibkr' prefix.
 * Supports both paper trading (port 7497) and live trading (port 7496) modes.
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "ibkr")
public class IbkrProperties {

    /**
     * Gateway hostname (default: localhost)
     */
    private String host = "localhost";

    /**
     * Gateway port: 7497 (paper trading) or 7496 (live trading)
     */
    private int port = 7497;

    /**
     * Client ID (unique per connection, default: 1)
     */
    private int clientId = 1;

    /**
     * Trading mode: PAPER or LIVE
     */
    private TradingMode mode = TradingMode.PAPER;

    /**
     * Connection timeout in seconds
     */
    private int connectionTimeoutSeconds = 30;

    /**
     * Reconnect delay in seconds
     */
    private int reconnectDelaySeconds = 5;

    /**
     * Maximum reconnection attempts (0 = unlimited)
     */
    private int maxReconnectAttempts = 0;

    /**
     * Maximum pending requests for connection pooling
     */
    private int maxPendingRequests = 100;

    /**
     * Enable automatic reconnection on disconnect
     */
    private boolean autoReconnect = true;

    /**
     * Request market data timeout in seconds
     */
    private int marketDataTimeoutSeconds = 10;

    /**
     * Request options chain timeout in seconds
     */
    private int optionsChainTimeoutSeconds = 30;

    /**
     * Order placement timeout in seconds
     */
    private int orderTimeoutSeconds = 10;

    @PostConstruct
    public void validate() {
        if (port != 7496 && port != 7497) {
            log.warn("IBKR port {} is not standard. Expected 7496 (live) or 7497 (paper)", port);
        }

        if (mode == TradingMode.PAPER && port == 7496) {
            log.warn("Trading mode is PAPER but port is set to 7496 (live). Using port 7496.");
        }

        if (mode == TradingMode.LIVE && port == 7497) {
            log.warn("Trading mode is LIVE but port is set to 7497 (paper). Consider using port 7496 for live trading.");
        }

        log.info("IBKR Configuration: host={}, port={}, mode={}, clientId={}",
                host, port, mode, clientId);
    }

    public enum TradingMode {
        PAPER,
        LIVE
    }
}