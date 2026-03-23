package com.trading.ibkr;

import com.trading.model.marketdata.MarketData;
import com.trading.model.marketdata.OptionsChain;
import com.trading.ibkr.client.IbkrConnectionManager;
import com.trading.ibkr.service.MarketDataService;
import com.trading.ibkr.service.OptionsDataService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * Integration tests for IBKR integration.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Connection lifecycle</li>
 *   <li>Market data retrieval</li>
 *   <li>Options chain retrieval</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b> IB Gateway must be running on localhost:7497
 */
@Slf4j
@SpringBootTest(classes = TestConfiguration.class)
@TestPropertySource(properties = {
    "ibkr.host=localhost",
    "ibkr.port=7497",
    "ibkr.mode=PAPER",
    "ibkr.auto-reconnect=false"
})
public class IbkrIntegrationTest {

    @Autowired
    private IbkrConnectionManager connectionManager;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private OptionsDataService optionsDataService;

    @Test
    public void testConnectionManagerExists() {
        // Verify connection manager bean is created
        org.junit.jupiter.api.Assertions.assertNotNull(connectionManager);
    }

    @Test
    public void testMarketDataServiceExists() {
        // Verify market data service bean is created
        org.junit.jupiter.api.Assertions.assertNotNull(marketDataService);
    }

    @Test
    public void testOptionsDataServiceExists() {
        // Verify options data service bean is created
        org.junit.jupiter.api.Assertions.assertNotNull(optionsDataService);
    }

    @Test
    public void testConnectionState() {
        // Verify connection state is accessible
        IbkrConnectionManager.ConnectionState state = connectionManager.getState();
        org.junit.jupiter.api.Assertions.assertNotNull(state);
        log.info("Connection state: {}", state);
    }

    /**
     * Test market data retrieval for AAPL.
     * Requires IB Gateway connection.
     */
    @Test
    public void testGetMarketData_AAPL() {
        Mono<MarketData> result = marketDataService.getMarketData("AAPL");

        StepVerifier.create(result)
                .assertNext(data -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(data);
                    org.junit.jupiter.api.Assertions.assertEquals("AAPL", data.getSymbol());
                    log.info("Market data for AAPL: lastPrice={}, bid={}, ask={}",
                            data.getLastPrice(), data.getBidPrice(), data.getAskPrice());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    /**
     * Test market data retrieval for TSLA.
     * Requires IB Gateway connection.
     */
    @Test
    public void testGetMarketData_TSLA() {
        Mono<MarketData> result = marketDataService.getMarketData("TSLA");

        StepVerifier.create(result)
                .assertNext(data -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(data);
                    org.junit.jupiter.api.Assertions.assertEquals("TSLA", data.getSymbol());
                    log.info("Market data for TSLA: lastPrice={}, bid={}, ask={}",
                            data.getLastPrice(), data.getBidPrice(), data.getAskPrice());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    /**
     * Test market data retrieval for SPY.
     * Requires IB Gateway connection.
     */
    @Test
    public void testGetMarketData_SPY() {
        Mono<MarketData> result = marketDataService.getMarketData("SPY");

        StepVerifier.create(result)
                .assertNext(data -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(data);
                    org.junit.jupiter.api.Assertions.assertEquals("SPY", data.getSymbol());
                    log.info("Market data for SPY: lastPrice={}, bid={}, ask={}",
                            data.getLastPrice(), data.getBidPrice(), data.getAskPrice());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }

    /**
     * Test options chain retrieval for SPY.
     * Requires IB Gateway connection.
     */
    @Test
    public void testGetOptionsChain_SPY() {
        Mono<OptionsChain> result = optionsDataService.getOptionsChain("SPY");

        StepVerifier.create(result)
                .assertNext(chain -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(chain);
                    org.junit.jupiter.api.Assertions.assertEquals("SPY", chain.getUnderlying());
                    org.junit.jupiter.api.Assertions.assertTrue(chain.getCalls().size() > 0);
                    org.junit.jupiter.api.Assertions.assertTrue(chain.getPuts().size() > 0);
                    log.info("Options chain for SPY: {} calls, {} puts, underlyingPrice={}",
                            chain.getCalls().size(), chain.getPuts().size(), chain.getUnderlyingPrice());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(60));
    }

    /**
     * Test options chain retrieval for AAPL.
     * Requires IB Gateway connection.
     */
    @Test
    public void testGetOptionsChain_AAPL() {
        Mono<OptionsChain> result = optionsDataService.getOptionsChain("AAPL");

        StepVerifier.create(result)
                .assertNext(chain -> {
                    org.junit.jupiter.api.Assertions.assertNotNull(chain);
                    org.junit.jupiter.api.Assertions.assertEquals("AAPL", chain.getUnderlying());
                    org.junit.jupiter.api.Assertions.assertTrue(chain.getCalls().size() > 0);
                    org.junit.jupiter.api.Assertions.assertTrue(chain.getPuts().size() > 0);
                    log.info("Options chain for AAPL: {} calls, {} puts, underlyingPrice={}",
                            chain.getCalls().size(), chain.getPuts().size(), chain.getUnderlyingPrice());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(60));
    }

    /**
     * Test error handling for invalid symbol.
     */
    @Test
    public void testInvalidSymbol_Handling() {
        Mono<MarketData> result = marketDataService.getMarketData("INVALIDXYZ");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> {
                    log.info("Expected error for invalid symbol: {}", throwable.getMessage());
                    return throwable instanceof Exception;
                })
                .verify(Duration.ofSeconds(10));
    }
}
