package com.trading.ibkr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring configuration for IBKR Integration module.
 *
 * <p>Enables:
 * <ul>
 *   <li>Component scanning</li>
 *   <li>Scheduling for connection health checks</li>
 *   <li>Async processing</li>
 * </ul>
 */
@Configuration
@ComponentScan(basePackages = "com.trading.ibkr")
@EnableScheduling
@EnableAsync
public class IbkrIntegrationConfig {
    // Configuration loaded from application.yml
}
