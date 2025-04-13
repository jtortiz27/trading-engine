package com.trading.util;

import com.trading.model.StockFeatures;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

public class LiveMarketFetcher {

    public static StockFeatures fetch(String symbol) {
        try {
            String token = System.getenv("IEX_CLOUD_TOKEN"); // Set this in your env
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
