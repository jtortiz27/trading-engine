package com.trading.ibkr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for IBKR Client Portal Web API.
 */
@Configuration
@ConfigurationProperties(prefix = "ibkr.client-portal")
public class ClientPortalConfig {

    /**
     * Base URL for Client Portal API.
     * Default: https://localhost:5000 (local Client Portal Gateway)
     */
    private String baseUrl = "https://localhost:5000";

    /**
     * API version path.
     * Default: /v1/api
     */
    private String apiPath = "/v1/api";

    /**
     * Username for authentication.
     */
    private String username;

    /**
     * Password for authentication.
     */
    private String password;

    /**
     * Account ID for trading operations.
     */
    private String accountId;

    /**
     * Connection timeout in seconds.
     */
    private int connectionTimeoutSeconds = 10;

    /**
     * Read timeout in seconds.
     */
    private int readTimeoutSeconds = 30;

    /**
     * Whether to trust all SSL certificates (for self-signed CP gateway).
     * Default: true (for local development)
     */
    private boolean trustAllSsl = true;

    /**
     * Retry attempts for failed requests.
     */
    private int retryAttempts = 3;

    /**
     * Delay between retries in milliseconds.
     */
    private long retryDelayMs = 1000;

    // Getters and Setters

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public boolean isTrustAllSsl() {
        return trustAllSsl;
    }

    public void setTrustAllSsl(boolean trustAllSsl) {
        this.trustAllSsl = trustAllSsl;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    /**
     * Get full API URL.
     */
    public String getFullApiUrl() {
        return baseUrl + apiPath;
    }
}
