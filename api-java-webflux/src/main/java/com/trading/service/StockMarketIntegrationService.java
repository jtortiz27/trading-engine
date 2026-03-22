package com.trading.service;

import com.trading.api.resource.polygon.stock.ExponentialMovingAverageResource;
import com.trading.api.resource.polygon.index.IndexSnapshotResource;
import com.trading.api.resource.polygon.stock.MACDResource;
import com.trading.api.resource.polygon.option.OptionContractSnapshotResource;
import com.trading.api.resource.polygon.stock.RSIResource;
import com.trading.api.resource.polygon.stock.StockAggregateBarsResource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMarketIntegrationService {

    @Value("${polygon.api.baseUrl}")
    private final String baseUrl;
    @Value("${polygon.api.quotesUrl}")
    private final String quotesPath;
    @Value("${polygon.api.stocksUrl}")
    private final String stocksPath;
    @Value("${polygon.api.indexUrl}")
    private final String indicesPath;
    @Value("${polygon.api.optionsUrl}")
    private final String optionsPath;
    @Value("${polygon.api.key}")
    private String token;

    private final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

    private final WebClient webClient;

    @SneakyThrows
    public Mono<MACDResource> retrieveMACDForTicker(String ticker) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(quotesPath + ticker)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();

        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(MACDResource.class);
    }

    @SneakyThrows
    public Mono<ExponentialMovingAverageResource> retrieveEMAForTicker(String ticker) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(quotesPath + ticker)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();

        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(ExponentialMovingAverageResource.class);
    }

    @SneakyThrows
    public Mono<IndexSnapshotResource> retrieveIndexSnapshot(String ticker) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(indicesPath + ticker)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();

        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(IndexSnapshotResource.class);
    }

    @SneakyThrows
    public Mono<OptionContractSnapshotResource> retrieveOptionContractSnapshot(String ticker) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(optionsPath + ticker)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();

        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(OptionContractSnapshotResource.class);
    }

    @SneakyThrows
    public Mono<RSIResource> retrieveRSIForTicker(String symbol) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(stocksPath)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();
        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(RSIResource.class);
    }

    @SneakyThrows
    public Mono<StockAggregateBarsResource> retrieveCustomBarsForTicker(String symbol) {
        String requestUri = uriComponentsBuilder
                .uri(new URI(baseUrl))
                .path(stocksPath)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .queryParam("apiKey", token)
                .build()
                .toUriString();

        return webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(StockAggregateBarsResource.class);
    }
}
