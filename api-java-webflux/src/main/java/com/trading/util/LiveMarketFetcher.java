package com.trading.util;

import com.trading.model.StockFeatures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LiveMarketFetcher {

  @Value("${api.key}")
  private String token;

  @Value("${api.url}")
  private String apiUrl;

  public StockFeatures fetch(String symbol) {
    try {
      String url = "https://cloud.iexapis.com/stable/stock/" + symbol + "/quote?token=" + token;

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      // Example of a fake value — should parse real JSON instead
      StockFeatures features = new StockFeatures();
      features.setSymbol(symbol);
      features.setPriceChange(Math.random() * 5 - 2.5); // Stub
      features.setNewsSentiment("neutral"); // Can hook up sentiment later
      features.setTimestamp(LocalDateTime.now());

      return features;

    } catch (Exception e) {
      throw new RuntimeException("Error fetching market data", e);
    }
  }
}
