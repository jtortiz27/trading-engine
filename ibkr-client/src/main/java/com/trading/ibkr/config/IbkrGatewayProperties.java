package com.trading.ibkr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for IBKR Gateway connection.
 */
@Configuration
@ConfigurationProperties(prefix = "ibkr.gateway")
public class IbkrGatewayProperties {
  private String host = "127.0.0.1";
  private int port = 7497; // 7497 = paper, 7496 = live
  private int clientId = 0;
  private int connectionTimeoutSeconds = 10;
  private boolean paperTrading = true;
  private int reconnectAttempts = 5;
  private long reconnectDelayMs = 5000;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getClientId() {
    return clientId;
  }

  public void setClientId(int clientId) {
    this.clientId = clientId;
  }

  public int getConnectionTimeoutSeconds() {
    return connectionTimeoutSeconds;
  }

  public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
    this.connectionTimeoutSeconds = connectionTimeoutSeconds;
  }

  public boolean isPaperTrading() {
    return paperTrading;
  }

  public void setPaperTrading(boolean paperTrading) {
    this.paperTrading = paperTrading;
  }

  public int getReconnectAttempts() {
    return reconnectAttempts;
  }

  public void setReconnectAttempts(int reconnectAttempts) {
    this.reconnectAttempts = reconnectAttempts;
  }

  public long getReconnectDelayMs() {
    return reconnectDelayMs;
  }

  public void setReconnectDelayMs(long reconnectDelayMs) {
    this.reconnectDelayMs = reconnectDelayMs;
  }
}
