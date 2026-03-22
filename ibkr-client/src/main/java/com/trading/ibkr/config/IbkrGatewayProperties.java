package com.trading.ibkr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
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
}
