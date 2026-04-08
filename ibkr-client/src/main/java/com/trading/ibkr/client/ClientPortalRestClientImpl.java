package com.trading.ibkr.client;

import com.trading.ibkr.config.ClientPortalConfig;
import com.trading.ibkr.model.Order;
import com.trading.ibkr.model.OptionsChain;
import com.trading.ibkr.model.TickData;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of ClientPortalRestClient using Spring WebClient.
 *
 * <p>The Client Portal Web API requires:
 * <ol>
 *   <li>Client Portal Gateway running locally (port 5000 by default)</li>
 *   <li>Authentication via SSO</li>
 *   <li>Session cookie management</li>
 * </ol>
 *
 * <p>For headless operation, the CP Gateway must be running and authenticated.
 */
@Slf4j
@Component
public class ClientPortalRestClientImpl implements ClientPortalRestClient {

    private final ClientPortalConfig config;
    private final WebClient webClient;
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final Map<String, TickData> marketDataCache = new ConcurrentHashMap<>();

    public ClientPortalRestClientImpl(ClientPortalConfig config) throws SSLException {
        this.config = config;
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() throws SSLException {
        HttpClient httpClient;

        if (config.isTrustAllSsl()) {
            // For self-signed certificates (local CP gateway)
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));
        } else {
            httpClient = HttpClient.create();
        }

        return WebClient.builder()
                .baseUrl(config.getFullApiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Mono<Void> authenticate(String username, String password) {
        log.info("Authenticating with Client Portal...");

        // CP API authentication is via /portal/sso/validate
        // This requires the CP Gateway to handle the login flow
        // For headless, we assume CP Gateway is already authenticated
        // or use the competitive login endpoint

        return webClient.post()
                .uri("/portal/sso/validate")
                .bodyValue(Map.of(
                        "username", username,
                        "password", password
                ))
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    authenticated.set(true);
                    log.info("Successfully authenticated with Client Portal");
                })
                .doOnError(error -> log.error("Authentication failed: {}", error.getMessage()))
                .then();
    }

    @Override
    public Mono<Boolean> isAuthenticated() {
        return Mono.just(authenticated.get());
    }

    @Override
    public Mono<List<String>> getAccounts() {
        return webClient.get()
                .uri("/portfolio/accounts")
                .retrieve()
                .bodyToMono(List.class)
                .cast(List.class)
                .map(accounts -> {
                    // Extract account IDs from response
                    return accounts.stream()
                            .map(acc -> ((Map<String, Object>) acc).get("accountId").toString())
                            .toList();
                });
    }

    @Override
    public Mono<List<Map<String, Object>>> getPortfolio() {
        return webClient.get()
                .uri("/portfolio/{accountId}/positions", config.getAccountId())
                .retrieve()
                .bodyToMono(List.class)
                .cast(List.class);
    }

    @Override
    public Mono<TickData> getMarketData(String symbol) {
        // CP API: GET /iserver/marketdata/snapshot
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/iserver/marketdata/snapshot")
                        .queryParam("conids", getConId(symbol))  // Need conid lookup
                        .queryParam("fields", "31,55,83,84,86,88")  // Bid, Ask, Last, etc.
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToTickData);
    }

    @Override
    public Flux<TickData> streamMarketData(String symbol) {
        // CP API supports Server-Sent Events for streaming
        // This is a simplified implementation
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> getMarketData(symbol))
                .distinctUntilChanged(TickData::lastPrice)
                .onErrorContinue((err, obj) -> log.warn("Stream error: {}", err.getMessage()));
    }

    @Override
    public Mono<OptionsChain> getOptionsChain(String symbol) {
        // CP API: POST /iserver/secdef/search for contract search
        // Then GET /iserver/secdef/info for strikes/expirations
        return webClient.post()
                .uri("/iserver/secdef/search")
                .bodyValue(Map.of(
                        "symbol", symbol,
                        "secType", "OPT"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToOptionsChain);
    }

    @Override
    public Mono<Map<String, Object>> placeOrder(Order order) {
        return webClient.post()
                .uri("/iserver/account/{accountId}/orders", config.getAccountId())
                .bodyValue(mapOrderToRequest(order))
                .retrieve()
                .bodyToMono(Map.class);
    }

    @Override
    public Mono<Map<String, Object>> getOrderStatus(String orderId) {
        return webClient.get()
                .uri("/iserver/account/orders")
                .retrieve()
                .bodyToMono(Map.class);
    }

    @Override
    public Mono<Void> cancelOrder(String orderId) {
        return webClient.delete()
                .uri("/iserver/account/{accountId}/order/{orderId}",
                        config.getAccountId(), orderId)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    @Override
    public Mono<Void> logout() {
        return webClient.post()
                .uri("/logout")
                .retrieve()
                .toBodilessEntity()
                .doFinally(sig -> authenticated.set(false))
                .then();
    }

    // Helper methods

    private Long getConId(String symbol) {
        // TODO: Implement conid lookup via /iserver/secdef/search
        // This requires caching conid to symbol mapping
        return 0L;  // Placeholder
    }

    private TickData mapToTickData(Map<String, Object> data) {
        // Map CP API response to TickData model
        return new TickData();
    }

    private OptionsChain mapToOptionsChain(Map<String, Object> data) {
        // Map CP API response to OptionsChain model
        return new OptionsChain();
    }

    private Map<String, Object> mapOrderToRequest(Order order) {
        // Map Order model to CP API request format
        return Map.of(
                "conid", getConId(order.getSymbol()),
                "orderType", order.getOrderType(),
                "quantity", order.getQuantity(),
                "side", order.getSide()
        );
    }
}
