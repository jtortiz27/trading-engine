package com.trading.ibkr.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for IBKR Client Portal REST API client.
 * 
 * Properties should be defined in application.yml:
 * <pre>
 * ibkr:
 *   client-portal:
 *     base-url: https://localhost:5000
 *     api-path: /v1/api
 *     connect-timeout: 10s
 *     read-timeout: 30s
 *     write-timeout: 30s
 *     max-retries: 3
 *     retry-delay: 1s
 *     ssl-verification: false  # Set to true for production with valid cert
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ibkr.client-portal")
public class ClientPortalConfig {
    
    /**
     * Base URL for the Client Portal Gateway.
     * Default: https://localhost:5000
     */
    @NotBlank
    private String baseUrl = "https://localhost:5000";
    
    /**
     * API path prefix.
     * Default: /v1/api
     */
    @NotBlank
    private String apiPath = "/v1/api";
    
    /**
     * Connection timeout.
     * Default: 10 seconds
     */
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(10);
    
    /**
     * Read timeout for API calls.
     * Default: 30 seconds
     */
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(30);
    
    /**
     * Write timeout for API calls.
     * Default: 30 seconds
     */
    @NotNull
    private Duration writeTimeout = Duration.ofSeconds(30);
    
    /**
     * Maximum number of retries for failed requests.
     * Default: 3
     */
    private int maxRetries = 3;
    
    /**
     * Delay between retries.
     * Default: 1 second
     */
    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);
    
    /**
     * Whether to verify SSL certificates.
     * Set to false for local development with self-signed certs.
     * Default: false
     */
    private boolean sslVerification = false;
    
    /**
     * Maximum buffer size for WebClient (in bytes).
     * Default: 16MB
     */
    private int maxBufferSize = 16 * 1024 * 1024;
    
    /**
     * Whether to enable request/response logging.
     * Default: false
     */
    private boolean enableLogging = false;
    
    /**
     * Get the full API base URL.
     * 
     * @return base URL with API path appended
     */
    public String getFullApiUrl() {
        return baseUrl + apiPath;
    }
    
    /**
     * Authentication endpoint path.
     * 
     * @return authentication endpoint URL
     */
    public String getAuthUrl() {
        return baseUrl + "/portal/sso/validate";
    }
    
    /**
     * Logout endpoint path.
     * 
     * @return logout endpoint URL
     */
    public String getLogoutUrl() {
        return baseUrl + apiPath + "/logout";
    }
}