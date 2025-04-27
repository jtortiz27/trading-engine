package com.trading.util;

import com.trading.api.resource.polygon.ExponentialMovingAverageResource;
import com.trading.model.StockFeatures;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class LiveMarketFetcher {

    @Value("${api.key}")
    private String token;

    @Value("${api.url}")
    private String apiUrl;

    private final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

    //Injected at Runtime
    private final WebClient webClient;
    private final PolygonToStockFeatureMapper mapper;

    @SneakyThrows
    public List<StockFeatures> fetch(String symbol) {
        String uri = uriComponentsBuilder
                .uri(new URI(apiUrl))
                .path(symbol)
                .queryParam("apiKey", token)
                .queryParam("sort", "timestamp")
                .queryParam("order", "asc")
                .build()
                .toUriString();
        ExponentialMovingAverageResource response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(ExponentialMovingAverageResource.class)
                .block();

        return !isEmpty(response) ? mapper.fromEmaResource(response) : Collections.emptyList();
    }
}
