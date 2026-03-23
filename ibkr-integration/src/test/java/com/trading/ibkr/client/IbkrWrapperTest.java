package com.trading.ibkr.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IbkrWrapper}.
 */
class IbkrWrapperTest {

    private IbkrWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new IbkrWrapper();
    }

    @Test
    void testConnectAck_CompletesConnectionFuture() throws Exception {
        // Given
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        wrapper.setConnectionFuture(future);

        // When
        wrapper.connectAck();

        // Then
        assertTrue(future.get(1, TimeUnit.SECONDS));
        assertTrue(wrapper.isConnected());
    }

    @Test
    void testTickPrice_UpdatesTickData() {
        // Given
        int tickerId = 1;
        com.ib.client.TickAttrib attribs = new com.ib.client.TickAttrib();

        // When
        wrapper.tickPrice(tickerId, com.ib.client.TickType.LAST, 150.50, attribs);
        wrapper.tickPrice(tickerId, com.ib.client.TickType.BID, 150.45, attribs);
        wrapper.tickPrice(tickerId, com.ib.client.TickType.ASK, 150.55, attribs);

        // Then
        IbkrWrapper.TickData data = wrapper.getTickDataMap().get(tickerId);
        assertNotNull(data);
        assertEquals(150.50, data.getLastPrice(), 0.001);
        assertEquals(150.45, data.getBidPrice(), 0.001);
        assertEquals(150.55, data.getAskPrice(), 0.001);
    }

    @Test
    void testTickOptionComputation_UpdatesGreeks() {
        // Given
        int tickerId = 1;
        double delta = 0.55;
        double gamma = 0.04;
        double theta = -0.15;
        double vega = 0.20;
        double iv = 0.25;
        double underlyingPrice = 150.0;

        // When
        wrapper.tickOptionComputation(tickerId, com.ib.client.TickType.MODEL_OPTION,
                0, iv, delta, 2.50, 0.0, gamma, vega, theta, underlyingPrice);

        // Then
        IbkrWrapper.TickData data = wrapper.getTickDataMap().get(tickerId);
        assertNotNull(data);
        IbkrWrapper.OptionGreeks greeks = data.getGreeks();
        assertNotNull(greeks);
        assertEquals(delta, greeks.getDelta(), 0.001);
        assertEquals(gamma, greeks.getGamma(), 0.001);
        assertEquals(theta, greeks.getTheta(), 0.001);
        assertEquals(vega, greeks.getVega(), 0.001);
        assertEquals(iv, greeks.getImpliedVolatility(), 0.001);
        assertEquals(underlyingPrice, greeks.getUnderlyingPrice(), 0.001);
    }

    @Test
    void testOrderStatus_UpdatesOrderData() {
        // Given
        int orderId = 100;
        String status = "Filled";
        com.ib.client.Decimal filled = com.ib.client.Decimal.get(100);
        com.ib.client.Decimal remaining = com.ib.client.Decimal.get(0);
        double avgFillPrice = 150.50;

        CompletableFuture<IbkrWrapper.OrderStatusData> future = new CompletableFuture<>();
        wrapper.getOrderFutures().put(orderId, future);

        // When
        wrapper.orderStatus(orderId, status, filled, remaining, avgFillPrice,
                0, 0, 0.0, 1, "", 0.0);

        // Then
        IbkrWrapper.OrderStatusData data = wrapper.getOrderStatusMap().get(orderId);
        assertNotNull(data);
        assertEquals(status, data.getStatus());
        assertEquals(100L, data.getFilled());
        assertEquals(0L, data.getRemaining());
        assertEquals(avgFillPrice, data.getAvgFillPrice(), 0.001);

        // Future should be completed
        assertTrue(future.isDone());
    }

    @Test
    void testManagedAccounts_SetsAccountId() {
        // Given
        String accounts = "DU123456";

        // When
        wrapper.managedAccounts(accounts);

        // Then
        assertEquals("DU123456", wrapper.getAccountId());
    }

    @Test
    void testConnectionClosed_SetsDisconnected() {
        // Given
        wrapper.connectAck();
        assertTrue(wrapper.isConnected());

        // When
        wrapper.connectionClosed();

        // Then
        assertFalse(wrapper.isConnected());
    }
}
