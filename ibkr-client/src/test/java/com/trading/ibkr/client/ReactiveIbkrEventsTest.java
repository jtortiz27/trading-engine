package com.trading.ibkr.client;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IBKR reactive event classes.
 */
class ReactiveIbkrEventsTest {

    @Test
    void tickEvent_shouldBuildCorrectly() {
        TickEvent event = TickEvent.builder()
            .tickerId(1)
            .tickType(TickEvent.TickType.LAST_PRICE)
            .value(150.0)
            .size(100L)
            .build();

        assertEquals(Integer.valueOf(1), event.getTickerId());
        assertEquals(TickEvent.TickType.LAST_PRICE, event.getTickType());
        assertEquals(Double.valueOf(150.0), event.getValue());
        assertEquals(Long.valueOf(100L), event.getSize());
    }

    @Test
    void orderStatusEvent_shouldBuildCorrectly() {
        OrderStatusEvent event = OrderStatusEvent.builder()
            .orderId(123)
            .status(OrderStatusEvent.OrderStatus.SUBMITTED)
            .filled(100L)
            .remaining(0L)
            .avgFillPrice(150.0)
            .build();

        assertEquals(Integer.valueOf(123), event.getOrderId());
        assertEquals(OrderStatusEvent.OrderStatus.SUBMITTED, event.getStatus());
        assertEquals(Long.valueOf(100L), event.getFilled());
    }

    @Test
    void optionsChainEvent_intermediate_shouldCreateCorrectly() {
        java.util.Set<String> expirations = java.util.Set.of("2024-03-15", "2024-04-15");
        java.util.Set<Double> strikes = java.util.Set.of(140.0, 145.0, 150.0);

        OptionsChainEvent event = OptionsChainEvent.intermediate(
            1, "AAPL", 265598, "SMART", "AAPL", "100",
            expirations, strikes
        );

        assertEquals(Integer.valueOf(1), event.getRequestId());
        assertEquals("AAPL", event.getUnderlyingSymbol());
        assertEquals(Integer.valueOf(265598), event.getUnderlyingConId());
        assertEquals(Boolean.FALSE, event.getComplete());
        assertEquals(expirations, event.getExpirations());
    }

    @Test
    void optionsChainEvent_complete_shouldCreateCorrectly() {
        OptionsChainEvent event = OptionsChainEvent.complete(1);

        assertEquals(Integer.valueOf(1), event.getRequestId());
        assertEquals(Boolean.TRUE, event.getComplete());
        assertNull(event.getExpirations());
    }

    @Test
    void contractDetailsEvent_shouldCreateCorrectly() {
        ContractDetailsEvent event = ContractDetailsEvent.complete(1);

        assertEquals(1, event.getRequestId());
        assertTrue(event.isComplete());
    }

    @Test
    void tickEvent_tickTypeMapping_shouldMapCorrectly() {
        assertEquals(TickEvent.TickType.BID_PRICE, TickEvent.TickType.valueOf("BID_PRICE"));
        assertEquals(TickEvent.TickType.ASK_SIZE, TickEvent.TickType.valueOf("ASK_SIZE"));
        assertEquals(TickEvent.TickType.UNKNOWN, TickEvent.TickType.valueOf("UNKNOWN"));
    }
}
