package com.trading.ibkr;

import com.trading.ibkr.config.IbkrIntegrationConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Test configuration for IBKR integration tests.
 */
@SpringBootApplication
@Import(IbkrIntegrationConfig.class)
public class TestConfiguration {
    // Test configuration
}
