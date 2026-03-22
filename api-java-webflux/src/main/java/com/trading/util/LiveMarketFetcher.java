//package com.trading.util;
//
//import com.trading.api.resource.polygon.stock.ExponentialMovingAverageResource;
//import com.trading.api.resource.polygon.stock.MACDResource;
//import com.trading.model.StockFeatures;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//import java.net.URI;
//import java.util.Collections;
//import java.util.List;
//
//import static org.springframework.util.ObjectUtils.isEmpty;
//
//@Component
//@RequiredArgsConstructor
//public class LiveMarketFetcher {
//
//
//
//    @Value("${api.url}")
//    private String apiUrl;
//
//
//    //Injected at Runtime
//    private final WebClient webClient;
//    private final PolygonToStockFeatureMapper mapper;
//
//    @SneakyThrows
//    public List<StockFeatures> fetch(String symbol) {
//        String emaUri = uriComponentsBuilder
//                .uri(new URI(apiUrl))
//                .path(symbol)
//                .queryParam("apiKey", token)
//                .queryParam("sort", "timestamp")
//                .queryParam("order", "asc")
//                .build()
//                .toUriString();
//
//        Mono<ExponentialMovingAverageResource> emaResourceMono = webClient.get()
//                .uri(emaUri)
//                .retrieve()
//                .bodyToMono(ExponentialMovingAverageResource.class);
//
//        String macdUri = uriComponentsBuilder
//                .uri(new URI(apiUrl))
//                .path(symbol)
//                .queryParam("apiKey", token)
//                .queryParam("sort", "timestamp")
//                .queryParam("order", "asc")
//                .build()
//                .toUriString();
//
//        Mono<MACDResource> macdResourceMono = webClient.get()
//                .uri(macdUri)
//                .retrieve()
//                .bodyToMono(MACDResource.class);
//
//        Mono.zip(emaResourceMono, macdResourceMono, (emaResource, macdResource) -> {
//            List<StockFeatures> stockFeatures = !isEmpty(emaResource) ? mapper.fromEmaResource(emaResource) : Collections.emptyList();
//
//        }).then();
//
//        return;
//    }
//}
