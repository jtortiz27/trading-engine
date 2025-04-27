package com.trading.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class WebfluxConfig {

    private ObjectWriter PRETTY_PRINTER = null;
    private static final int MAX_LOGGABLE_BODY_SIZE = 4096; // 4 KB

    private final ObjectMapper objectMapper;

    @Value("${model-server.url}")
    private String modelServerUrl;

    @Bean
    @Profile("dev") // Only activate body logging filter in dev mode
    public WebFilter serverRequestLoggingFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getHeaders().getContentLength() > 0 || request.getHeaders().getContentType() != null) {
                return DataBufferUtils.join(request.getBody())
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);

                            if (bytes.length > MAX_LOGGABLE_BODY_SIZE) {
                                System.out.println("Request body too large to log fully (" + bytes.length + " bytes). Logging skipped.");
                            } else {
                                logBody(request, bytes);
                            }

                            Flux<DataBuffer> cachedBodyFlux = Flux.defer(() -> {
                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                                return Mono.just(buffer);
                            });

                            ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return cachedBodyFlux;
                                }
                            };

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });
            }

            return chain.filter(exchange);
        };
    }

    private void logBody(ServerHttpRequest request, byte[] bodyBytes) {
        if (PRETTY_PRINTER == null) {
            PRETTY_PRINTER = objectMapper.writerWithDefaultPrettyPrinter();
        }
        System.out.println("Incoming Request: " + request.getMethod() + " " + request.getURI());

        if (isJson(request)) {
            try {
                Object json = deserialize(bodyBytes, Object.class);
                String prettyJson = PRETTY_PRINTER.writeValueAsString(json);
                System.out.println("Request Body (Pretty JSON):\n" + prettyJson);
            } catch (Exception e) {
                System.out.println("Failed to pretty-print JSON. Logging raw body:");
                System.out.println(new String(bodyBytes, StandardCharsets.UTF_8));
            }
        } else {
            System.out.println("Request Body (Raw Text):\n" + new String(bodyBytes, StandardCharsets.UTF_8));
        }
    }

    private boolean isJson(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null && (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || MediaType.APPLICATION_JSON_UTF8.isCompatibleWith(contentType));
    }

    private <T> T deserialize(byte[] jsonBytes, Class<T> returnType) throws Exception {
        JsonParser asyncParser = objectMapper.getFactory().createNonBlockingByteArrayParser();
        ByteArrayFeeder feeder = (ByteArrayFeeder) asyncParser.getNonBlockingInputFeeder();
        feeder.feedInput(jsonBytes, 0, jsonBytes.length);
        feeder.endOfInput();
        return objectMapper.readValue(asyncParser, returnType);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(modelServerUrl)
                .filter(logOutgoingRequest()) // Outgoing request logging
                .build();
    }

    private ExchangeFilterFunction logOutgoingRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            System.out.println("Outgoing Request: " + clientRequest.method() + " " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> System.out.println(name + ": " + value)));
            return Mono.just(clientRequest);
        });
    }
}