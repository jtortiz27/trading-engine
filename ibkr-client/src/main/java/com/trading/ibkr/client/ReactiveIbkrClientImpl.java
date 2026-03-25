package com.trading.ibkr.client;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.EClientSocket;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.trading.ibkr.config.IbkrGatewayProperties;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Production implementation of ReactiveIbkrClient.
 * Wraps IBKR's EClientSocket with reactive streams.
 */
@Slf4j
public class ReactiveIbkrClientImpl implements ReactiveIbkrClient, EWrapper {

    private final IbkrGatewayProperties properties;
    private EClientSocket clientSocket;
    private EReaderSignal readerSignal;
    private EReader reader;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger nextRequestId = new AtomicInteger(1);
    private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

    // Sinks for reactive streams
    private final Sinks.Many<Boolean> connectionSink = Sinks.many().multicast().directBestEffort();
    private final Map<Integer, FluxSink<TickEvent>> tickSinks = new ConcurrentHashMap<>();
    private final Map<Integer, FluxSink<OptionsChainEvent>> optionsChainSinks = new ConcurrentHashMap<>();
    private final Map<Integer, FluxSink<OrderStatusEvent>> orderStatusSinks = new ConcurrentHashMap<>();
    private final Map<Integer, FluxSink<ContractDetailsEvent>> contractDetailsSinks = new ConcurrentHashMap<>();
    private final Map<Integer, FluxSink<AccountSummaryEvent>> accountSummarySinks = new ConcurrentHashMap<>();

    public ReactiveIbkrClientImpl(IbkrGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> connect() {
        return Mono.fromRunnable(() -> {
                    log.info("Connecting to IBKR Gateway at {}:{}", 
                            properties.getHost(), properties.getPort());
                    
                    readerSignal = new EJavaSignal();
                    clientSocket = new EClientSocket(this);
                    clientSocket.eConnect(properties.getHost(), properties.getPort(), 
                            properties.getClientId());
                    
                    // Start reader thread
                    reader = new EReader(clientSocket, readerSignal);
                    reader.start();
                    
                    // Start message processing thread
                    Schedulers.boundedElastic().schedule(this::processMessages);
                })
                .then(Mono.fromFuture(connectionFuture))
                .timeout(java.time.Duration.ofSeconds(properties.getConnectionTimeoutSeconds()),
                        Mono.error(new RuntimeException("Connection to IBKR Gateway timed out")))
                .doOnSuccess(v -> log.info("Connected to IBKR Gateway"))
                .doOnError(e -> log.error("Failed to connect to IBKR Gateway", e));
    }

    private void processMessages() {
        while (clientSocket != null && clientSocket.isConnected()) {
            readerSignal.waitForSignal();
            try {
                reader.processMsgs();
            } catch (Exception e) {
                log.error("Error processing IBKR messages", e);
            }
        }
    }

    @Override
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            if (clientSocket != null && clientSocket.isConnected()) {
                log.info("Disconnecting from IBKR Gateway");
                clientSocket.eDisconnect();
                connected.set(false);
                connectionSink.tryEmitNext(false);
            }
        });
    }

    @Override
    public Mono<Boolean> isConnected() {
        return Mono.fromCallable(() -> connected.get() && clientSocket != null && clientSocket.isConnected());
    }

    @Override
    public Flux<Boolean> connectionStatus() {
        return connectionSink.asFlux();
    }

    @Override
    public Mono<Integer> getNextValidId() {
        return Mono.fromCallable(nextRequestId::get);
    }

    @Override
    public Flux<TickEvent> requestMarketData(int tickerId, Contract contract) {
        return Flux.create(sink -> {
            tickSinks.put(tickerId, sink);
            clientSocket.reqMktData(tickerId, contract, "", false, false, null);
            
            sink.onCancel(() -> {
                tickSinks.remove(tickerId);
                clientSocket.cancelMktData(tickerId);
            });
        });
    }

    @Override
    public Mono<Void> cancelMarketData(int tickerId) {
        return Mono.fromRunnable(() -> {
            FluxSink<TickEvent> sink = tickSinks.remove(tickerId);
            if (sink != null) {
                sink.complete();
            }
            clientSocket.cancelMktData(tickerId);
        });
    }

    @Override
    public Flux<OptionsChainEvent> requestOptionsChain(int underlyingConId, String underlyingSymbol) {
        int reqId = nextRequestId.getAndIncrement();
        return Flux.create(sink -> {
            optionsChainSinks.put(reqId, sink);
            clientSocket.reqSecDefOptParams(reqId, underlyingSymbol, "", "STK", underlyingConId);
            
            sink.onCancel(() -> {
                optionsChainSinks.remove(reqId);
            });
        });
    }

    @Override
    public Flux<OrderStatusEvent> placeOrder(int orderId, Contract contract, Order order) {
        return Flux.create(sink -> {
            orderStatusSinks.put(orderId, sink);
            clientSocket.placeOrder(orderId, contract, order);
            
            sink.onCancel(() -> {
                orderStatusSinks.remove(orderId);
            });
        });
    }

    @Override
    public Mono<Void> cancelOrder(int orderId) {
        return Mono.fromRunnable(() -> {
            FluxSink<OrderStatusEvent> sink = orderStatusSinks.get(orderId);
            if (sink != null) {
                sink.complete();
            }
            clientSocket.cancelOrder(orderId);
        });
    }

    @Override
    public Flux<ContractDetailsEvent> requestContractDetails(int reqId, Contract contract) {
        return Flux.create(sink -> {
            contractDetailsSinks.put(reqId, sink);
            clientSocket.reqContractDetails(reqId, contract);
            
            sink.onCancel(() -> {
                contractDetailsSinks.remove(reqId);
            });
        });
    }

    @Override
    public Flux<AccountSummaryEvent> requestAccountSummary(int reqId, String groupName, String tags) {
        return Flux.create(sink -> {
            accountSummarySinks.put(reqId, sink);
            clientSocket.reqAccountSummary(reqId, groupName, tags);
            
            sink.onCancel(() -> {
                accountSummarySinks.remove(reqId);
                clientSocket.cancelAccountSummary(reqId);
            });
        });
    }

    // ==================== EWrapper Implementation ====================

    @Override
    public void connected() {
        log.info("Connected to IBKR Gateway");
        connected.set(true);
    }

    @Override
    public void disconnected() {
        log.warn("Disconnected from IBKR Gateway");
        connected.set(false);
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Disconnected"));
        }
        connectionSink.tryEmitNext(false);
    }

    @Override
    public void nextValidId(int orderId) {
        log.info("Next valid order ID: {}", orderId);
        nextRequestId.set(orderId);
        if (!connectionFuture.isDone()) {
            connectionFuture.complete(null);
        }
        connectionSink.tryEmitNext(true);
    }

    @Override
    public void error(Exception e) {
        log.error("IBKR API error", e);
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(e);
        }
    }

    @Override
    public void error(String str) {
        log.error("IBKR API error: {}", str);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        log.error("IBKR Error - ID: {}, Code: {}, Message: {}", id, errorCode, errorMsg);
        if (errorCode == 502 && !connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(
                    new RuntimeException("Could not connect to IBKR Gateway: " + errorMsg));
        }
    }

    @Override
    public void connectionClosed() {
        log.warn("IBKR connection closed");
        connected.set(false);
        connectionSink.tryEmitNext(false);
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attrib) {
        FluxSink<TickEvent> sink = tickSinks.get(tickerId);
        if (sink != null) {
            TickEvent event = TickEvent.builder()
                    .tickerId(tickerId)
                    .tickType(mapTickType(field))
                    .value(price)
                    .attributes(mapTickAttributes(attrib))
                    .build();
            sink.next(event);
        }
    }

    @Override
    public void tickSize(int tickerId, int field, Decimal size) {
        FluxSink<TickEvent> sink = tickSinks.get(tickerId);
        if (sink != null) {
            TickEvent event = TickEvent.builder()
                    .tickerId(tickerId)
                    .tickType(mapTickType(field))
                    .size(size.longValue())
                    .build();
            sink.next(event);
        }
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        FluxSink<TickEvent> sink = tickSinks.get(tickerId);
        if (sink != null) {
            TickEvent event = TickEvent.builder()
                    .tickerId(tickerId)
                    .tickType(mapTickType(tickType))
                    .value(value)
                    .build();
            sink.next(event);
        }
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        FluxSink<TickEvent> sink = tickSinks.get(tickerId);
        if (sink != null) {
            TickEvent event = TickEvent.builder()
                    .tickerId(tickerId)
                    .tickType(mapTickType(tickType))
                    .stringValue(value)
                    .build();
            sink.next(event);
        }
    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
                                                       String tradingClass, String multiplier,
                                                       Set<String> expirations, Set<Double> strikes) {
        FluxSink<OptionsChainEvent> sink = optionsChainSinks.get(reqId);
        if (sink != null) {
            OptionsChainEvent event = OptionsChainEvent.intermediate(reqId, null, underlyingConId,
                    exchange, tradingClass, multiplier, expirations, strikes);
            sink.next(event);
        }
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {
        FluxSink<OptionsChainEvent> sink = optionsChainSinks.get(reqId);
        if (sink != null) {
            sink.next(OptionsChainEvent.complete(reqId));
            sink.complete();
            optionsChainSinks.remove(reqId);
        }
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice,
                            int clientId, String whyHeld, double mktCapPrice) {
        FluxSink<OrderStatusEvent> sink = orderStatusSinks.get(orderId);
        if (sink != null) {
            OrderStatusEvent event = OrderStatusEvent.builder()
                    .orderId(orderId)
                    .status(mapOrderStatus(status))
                    .filled(filled.longValue())
                    .remaining(remaining.longValue())
                    .avgFillPrice(avgFillPrice)
                    .lastFillPrice(lastFillPrice)
                    .whyHeld(whyHeld)
                    .marketCapPrice(mktCapPrice)
                    .build();
            sink.next(event);
        }
    }

    @Override
    public void contractDetails(int reqId, com.ib.client.ContractDetails contractDetails) {
        FluxSink<ContractDetailsEvent> sink = contractDetailsSinks.get(reqId);
        if (sink != null) {
            sink.next(ContractDetailsEvent.details(reqId, contractDetails));
        }
    }

    @Override
    public void contractDetailsEnd(int reqId) {
        FluxSink<ContractDetailsEvent> sink = contractDetailsSinks.get(reqId);
        if (sink != null) {
            sink.next(ContractDetailsEvent.complete(reqId));
            sink.complete();
            contractDetailsSinks.remove(reqId);
        }
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        FluxSink<AccountSummaryEvent> sink = accountSummarySinks.get(reqId);
        if (sink != null) {
            sink.next(AccountSummaryEvent.data(reqId, account, tag, value, currency));
        }
    }

    @Override
    public void accountSummaryEnd(int reqId) {
        FluxSink<AccountSummaryEvent> sink = accountSummarySinks.get(reqId);
        if (sink != null) {
            sink.next(AccountSummaryEvent.complete(reqId));
            sink.complete();
            accountSummarySinks.remove(reqId);
        }
    }

    // ==================== Helper Methods ====================

    private TickEvent.TickType mapTickType(int field) {
        return switch (field) {
            case 0 -> TickEvent.TickType.BID_SIZE;
            case 1 -> TickEvent.TickType.BID_PRICE;
            case 2 -> TickEvent.TickType.ASK_PRICE;
            case 3 -> TickEvent.TickType.ASK_SIZE;
            case 4 -> TickEvent.TickType.LAST_PRICE;
            case 5 -> TickEvent.TickType.LAST_SIZE;
            case 6 -> TickEvent.TickType.HIGH;
            case 7 -> TickEvent.TickType.LOW;
            case 8 -> TickEvent.TickType.VOLUME;
            case 9 -> TickEvent.TickType.CLOSE_PRICE;
            case 14 -> TickEvent.TickType.OPEN;
            case 37 -> TickEvent.TickType.MARK_PRICE;
            default -> TickEvent.TickType.UNKNOWN;
        };
    }

    private TickEvent.TickAttributes mapTickAttributes(TickAttrib attrib) {
        if (attrib == null) return null;
        return TickEvent.TickAttributes.builder()
                .canAutoExecute(attrib.canAutoExecute())
                .pastLimit(attrib.pastLimit())
                .preOpen(attrib.preOpen())
                .build();
    }

    private OrderStatusEvent.OrderStatus mapOrderStatus(String status) {
        if (status == null) return OrderStatusEvent.OrderStatus.UNKNOWN;
        try {
            return OrderStatusEvent.OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return OrderStatusEvent.OrderStatus.UNKNOWN;
        }
    }

    // ==================== Stub implementations for remaining EWrapper methods ====================
    
    @Override
    public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol,
                                        double delta, double optPrice, double pvDividend,
                                        double gamma, double vega, double theta, double undPrice) {}

    @Override
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
                        double impliedFuture, int holdDays, String futureLastTradeDate,
                        double dividendImpact, double dividendsToLastTradeDate) {}

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {}

    @Override
    public void tickSnapshotEnd(int reqId) {}

    @Override
    public void marketDataType(int reqId, int marketDataType) {}

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
                                   TickAttribLast tickAttribLast, String exchange, String specialConditions) {}

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice,
                                  Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {}

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {}

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {}

    @Override
    public void openOrderEnd() {}

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName) {}

    @Override
    public void updatePortfolio(Contract contract, Decimal position, double marketPrice,
                                 double marketValue, double averageCost, double unrealizedPNL,
                                 double realizedPNL, String accountName) {}

    @Override
    public void updateAccountTime(String timeStamp) {}

    @Override
    public void accountDownloadEnd(String accountName) {}

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {}

    @Override
    public void execDetailsEnd(int reqId) {}

    @Override
    public void updateMktDepth(int tickerId, int position, String operation, int side,
                                double price, Decimal size) {}

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation,
                                  int side, double price, Decimal size, boolean isSmartDepth) {}

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {}

    @Override
    public void managedAccounts(String accountsList) {}

    @Override
    public void receiveFA(int faDataType, String xml) {}

    @Override
    public void historicalData(int reqId, com.ib.client.Bar bar) {}

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {}

    @Override
    public void historicalDataUpdate(int reqId, com.ib.client.Bar bar) {}

    @Override
    public void scannerParameters(String xml) {}

    @Override
    public void scannerData(int reqId, int rank, com.ib.client.ContractDetails contractDetails,
                            String distance, String benchmark, String projection, String legsStr) {}

    @Override
    public void scannerDataEnd(int reqId) {}

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close,
                            Decimal volume, Decimal wap, int count) {}

    @Override
    public void currentTime(long time) {}

    @Override
    public void fundamentalData(int reqId, String data) {}

    @Override
    public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {}

    @Override
    public void commissionReport(com.ib.client.CommissionReport commissionReport) {}

    @Override
    public void position(String account, Contract contract, Decimal pos, double avgCost) {}

    @Override
    public void positionEnd() {}

    @Override
    public void verifyMessageAPI(String apiData) {}

    @Override
    public void verifyCompleted(boolean isSuccessful, String errorText) {}

    @Override
    public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {}

    @Override
    public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {}

    @Override
    public void displayGroupList(int reqId, String groups) {}

    @Override
    public void displayGroupUpdated(int reqId, String contractInfo) {}

    @Override
    public void positionMulti(int reqId, String account, String modelCode, Contract contract,
                              Decimal pos, double avgCost) {}

    @Override
    public void positionMultiEnd(int reqId) {}

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key,
                                    String value, String currency) {}

    @Override
    public void accountUpdateMultiEnd(int reqId) {}

    @Override
    public void softDollarTiers(int reqId, com.ib.client.SoftDollarTier[] tiers) {}

    @Override
    public void familyCodes(com.ib.client.FamilyCode[] familyCodes) {}

    @Override
    public void symbolSamples(int reqId, com.ib.client.ContractDescription[] contractDescriptions) {}

    @Override
    public void rerouteMktDataReq(int reqId, int conId, String exchange) {}

    @Override
    public void rerouteMktDepthReq(int reqId, int conId, String exchange) {}

    @Override
    public void marketRule(int marketRuleId, com.ib.client.PriceIncrement[] priceIncrements) {}

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL,
                          double realizedPnL, double value) {}

    @Override
    public void historicalTicks(int reqId, java.util.List<com.ib.client.HistoricalTick> ticks, boolean done) {}

    @Override
    public void historicalTicksBidAsk(int reqId, java.util.List<com.ib.client.HistoricalTickBidAsk> ticks, boolean done) {}

    @Override
    public void historicalTicksLast(int reqId, java.util.List<com.ib.client.HistoricalTickLast> ticks, boolean done) {}

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {}

    @Override
    public void completedOrdersEnd() {}

    @Override
    public void replaceFAEnd(int reqId, String text) {}

    @Override
    public void wshMetaData(int reqId, String dataJson) {}

    @Override
    public void wshEventData(int reqId, String dataJson) {}

    @Override
    public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone,
                                    java.util.List<com.ib.client.HistoricalSession> sessions) {}

    @Override
    public void userInfo(int reqId, String whiteBrandingId) {}

    @Override
    public void bondContractDetails(int reqId, com.ib.client.ContractDetails contractDetails) {}

    @Override
    public void connectAck() {}

    // Additional EWrapper methods for TWS API 9.81.1 compatibility
    @Override
    public void headTimestamp(int reqId, String headTimestamp) {}

    @Override
    public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {}

    @Override
    public void newsProviders(com.ib.client.NewsProvider[] newsProviders) {}

    @Override
    public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId,
                         String headline, String extraData) {}

    @Override
    public void newsArticle(int requestId, int articleType, String articleText) {}

    @Override
    public void historicalNews(int requestId, String time, String providerCode, String articleId,
                               String headline) {}

    @Override
    public void historicalNewsEnd(int requestId, boolean hasMore) {}

    @Override
    public void histogramData(int reqId, java.util.List<com.ib.client.HistogramEntry> items) {}
}
