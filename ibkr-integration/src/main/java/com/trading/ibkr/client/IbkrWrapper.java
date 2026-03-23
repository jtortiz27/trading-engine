package com.trading.ibkr.client;

import com.ib.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IBKR API callback handler implementation.
 *
 * <p>Processes all incoming messages from IBKR TWS/Gateway.
 * Stores received data in concurrent maps for thread-safe access.
 */
@Slf4j
public class IbkrWrapper implements EWrapper {

    @Setter
    private IbkrConnectionManager connectionManager;

    @Setter
    private CompletableFuture<Boolean> connectionFuture;

    @Getter
    private final Map<Integer, TickData> tickDataMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, CompletableFuture<TickData>> tickDataFutures = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, OptionsChainData> optionsChainMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, CompletableFuture<OptionsChainData>> optionsChainFutures = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, OrderStatusData> orderStatusMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, CompletableFuture<OrderStatusData>> orderFutures = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, ContractDetails> contractDetailsMap = new ConcurrentHashMap<>();

    @Getter
    private final Map<Integer, CompletableFuture<Void>> contractDetailsEndFutures = new ConcurrentHashMap<>();

    @Getter
    private volatile boolean connected = false;

    @Getter
    private volatile String accountId;

    // EWrapper implementation

    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
        TickData data = tickDataMap.computeIfAbsent(tickerId, k -> new TickData(tickerId));
        data.setField(field, price);

        if (field == TickType.LAST) {
            data.setLastPrice(price);
        } else if (field == TickType.BID) {
            data.setBidPrice(price);
        } else if (field == TickType.ASK) {
            data.setAskPrice(price);
        }

        CompletableFuture<TickData> future = tickDataFutures.get(tickerId);
        if (future != null && !future.isDone()) {
            future.complete(data);
        }

        log.debug("Tick price: tickerId={}, field={}, price={}", tickerId, field, price);
    }

    @Override
    public void tickSize(int tickerId, int field, Decimal size) {
        TickData data = tickDataMap.computeIfAbsent(tickerId, k -> new TickData(tickerId));
        long sizeValue = size.longValue();

        if (field == TickType.VOLUME) {
            data.setVolume(sizeValue);
        }

        log.debug("Tick size: tickerId={}, field={}, size={}", tickerId, field, sizeValue);
    }

    @Override
    public void tickOptionComputation(int tickerId, int tickType, int tickAttrib,
                                       double impliedVol, double delta, double optPrice,
                                       double pvDividend, double gamma, double vega,
                                       double theta, double undPrice) {
        TickData data = tickDataMap.computeIfAbsent(tickerId, k -> new TickData(tickerId));

        OptionGreeks greeks = new OptionGreeks();
        greeks.setImpliedVolatility(impliedVol);
        greeks.setDelta(delta);
        greeks.setGamma(gamma);
        greeks.setVega(vega);
        greeks.setTheta(theta);
        greeks.setUnderlyingPrice(undPrice);

        data.setGreeks(greeks);

        log.debug("Option computation: tickerId={}, delta={}, gamma={}, theta={}, vega={}",
                tickerId, delta, gamma, theta, vega);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        TickData data = tickDataMap.computeIfAbsent(tickerId, k -> new TickData(tickerId));

        // Handle VIX index
        if (tickType == TickType.INDEX_FUTURE_PREMIUM) {
            data.setVix(value);
        }

        log.debug("Tick generic: tickerId={}, tickType={}, value={}", tickerId, tickType, value);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value) {
        log.debug("Tick string: tickerId={}, tickType={}, value={}", tickerId, tickType, value);
    }

    @Override
    public void tickSnapshotEnd(int tickerId) {
        log.debug("Tick snapshot end: tickerId={}", tickerId);
    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
                                                      String tradingClass, String multiplier,
                                                      Set<String> expirations, Set<Double> strikes) {
        OptionsChainData chainData = optionsChainMap.computeIfAbsent(reqId, k -> new OptionsChainData(reqId));
        chainData.addExpirations(expirations);
        chainData.addStrikes(strikes);
        chainData.setTradingClass(tradingClass);
        chainData.setMultiplier(multiplier);

        log.debug("Security definition optional parameter: reqId={}, exchange={}, expirations={}",
                reqId, exchange, expirations.size());
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {
        log.info("Security definition optional parameter end: reqId={}", reqId);

        OptionsChainData chainData = optionsChainMap.get(reqId);
        CompletableFuture<OptionsChainData> future = optionsChainFutures.get(reqId);

        if (future != null && chainData != null && !future.isDone()) {
            future.complete(chainData);
        }
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        contractDetailsMap.put(reqId, contractDetails);
        log.debug("Contract details: reqId={}, symbol={}", reqId, contractDetails.contract().symbol());
    }

    @Override
    public void contractDetailsEnd(int reqId) {
        log.debug("Contract details end: reqId={}", reqId);

        CompletableFuture<Void> future = contractDetailsEndFutures.get(reqId);
        if (future != null && !future.isDone()) {
            future.complete(null);
        }
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        OrderStatusData statusData = new OrderStatusData(orderId, contract.symbol(), orderState.status());
        orderStatusMap.put(orderId, statusData);
        log.info("Open order: orderId={}, symbol={}, status={}", orderId, contract.symbol(), orderState.status());
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice,
                            int clientId, String whyHeld, double mktCapPrice) {
        OrderStatusData statusData = orderStatusMap.computeIfAbsent(orderId, k -> new OrderStatusData(orderId, "", status));
        statusData.setStatus(status);
        statusData.setFilled(filled.longValue());
        statusData.setRemaining(remaining.longValue());
        statusData.setAvgFillPrice(avgFillPrice);

        CompletableFuture<OrderStatusData> future = orderFutures.get(orderId);
        if (future != null && !future.isDone()) {
            // Complete on fill or final status
            if ("Filled".equals(status) || "Cancelled".equals(status) || "Inactive".equals(status)) {
                future.complete(statusData);
            }
        }

        log.info("Order status: orderId={}, status={}, filled={}, remaining={}",
                orderId, status, filled, remaining);
    }

    @Override
    public void nextValidId(int orderId) {
        log.info("Next valid order ID: {}", orderId);
    }

    @Override
    public void connectAck() {
        log.info("Connection acknowledged by TWS/Gateway");
        if (connectionFuture != null && !connectionFuture.isDone()) {
            connectionFuture.complete(true);
        }
        connected = true;
    }

    @Override
    public void connectionClosed() {
        log.warn("Connection closed by TWS/Gateway");
        connected = false;
        if (connectionManager != null) {
            connectionManager.disconnect();
        }
    }

    @Override
    public void error(Exception e) {
        log.error("API error: {}", e.getMessage(), e);
    }

    @Override
    public void error(String str) {
        log.error("API error: {}", str);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        log.error("API error: id={}, code={}, msg={}", id, errorCode, errorMsg);

        if (errorCode == 2104 || errorCode == 2106) {
            // Market data farm connection messages - not errors
            log.info("Market data farm status: {}", errorMsg);
        } else if (id > 0) {
            // Request-specific error - complete futures with error
            CompletableFuture<TickData> tickFuture = tickDataFutures.get(id);
            if (tickFuture != null && !tickFuture.isDone()) {
                tickFuture.completeExceptionally(new IbkrApiException(errorCode, errorMsg));
            }

            CompletableFuture<OptionsChainData> optionsFuture = optionsChainFutures.get(id);
            if (optionsFuture != null && !optionsFuture.isDone()) {
                optionsFuture.completeExceptionally(new IbkrApiException(errorCode, errorMsg));
            }
        }
    }

    @Override
    public void managedAccounts(String accountsList) {
        log.info("Managed accounts: {}", accountsList);
        if (accountsList != null && !accountsList.isEmpty()) {
            String[] accounts = accountsList.split(",");
            if (accounts.length > 0) {
                accountId = accounts[0];
            }
        }
    }

    // Required stub implementations

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName) {}

    @Override
    public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue,
                                 double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {}

    @Override
    public void updateAccountTime(String timeStamp) {}

    @Override
    public void accountDownloadEnd(String accountName) {}

    @Override
    public void openOrderEnd() {}

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {}

    @Override
    public void execDetailsEnd(int reqId) {}

    @Override
    public void commissionReport(CommissionReport commissionReport) {}

    @Override
    public void position(String account, Contract contract, Decimal pos, double avgCost) {}

    @Override
    public void positionEnd() {}

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {}

    @Override
    public void accountSummaryEnd(int reqId) {}

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {}

    @Override
    public void receiveFA(int faDataType, String xml) {}

    @Override
    public void historicalData(int reqId, Bar bar) {}

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {}

    @Override
    public void scannerParameters(String xml) {}

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark,
                            String projection, String legsStr) {}

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
    public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
                        double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
                        double dividendsToLastTradeDate) {}

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {}

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
    public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos,
                              double avgCost) {}

    @Override
    public void positionMultiEnd(int reqId) {}

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value,
                                   String currency) {}

    @Override
    public void accountUpdateMultiEnd(int reqId) {}

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
                                  TickAttribLast tickAttribLast, String exchange, String specialConditions) {}

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize,
                                  Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {}

    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {}

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {}

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
                                   List<HistoricalSession> sessions) {}

    @Override
    public void rerouteMktDataReq(int reqId, int conId, String exchange) {}

    @Override
    public void rerouteMktDepthReq(int reqId, int conId, String exchange) {}

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {}

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL,
                          double value) {}

    @Override
    public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean done) {}

    @Override
    public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {}

    @Override
    public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {}

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {}

    @Override
    public void headTimestamp(int reqId, String headTimestamp) {}

    @Override
    public void histogramData(int reqId, List<HistogramEntry> items) {}

    @Override
    public void historicalDataUpdate(int reqId, Bar bar) {}

    @Override
    public void rerouteMarketData(int reqId, int conId, String exchange) {}

    @Override
    public void rerouteMarketDepth(int reqId, int conId, String exchange) {}

    @Override
    public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {}

    // Inner classes for data storage

    @Getter
    @Setter
    public static class TickData {
        private final int tickerId;
        private String symbol;
        private double lastPrice;
        private double bidPrice;
        private double askPrice;
        private long volume;
        private double vix;
        private OptionGreeks greeks;

        public TickData(int tickerId) {
            this.tickerId = tickerId;
        }

        public void setField(int field, double value) {
            // Store additional tick fields if needed
        }
    }

    @Getter
    @Setter
    public static class OptionGreeks {
        private double impliedVolatility;
        private double delta;
        private double gamma;
        private double theta;
        private double vega;
        private double underlyingPrice;
    }

    @Getter
    @Setter
    public static class OptionsChainData {
        private final int reqId;
        private String symbol;
        private double underlyingPrice;
        private final Set<String> expirations = ConcurrentHashMap.newKeySet();
        private final Set<Double> strikes = ConcurrentHashMap.newKeySet();
        private String tradingClass;
        private String multiplier;
        private final Map<String, OptionContractData> contracts = new ConcurrentHashMap<>();

        public OptionsChainData(int reqId) {
            this.reqId = reqId;
        }

        public void addExpirations(Set<String> newExpirations) {
            expirations.addAll(newExpirations);
        }

        public void addStrikes(Set<Double> newStrikes) {
            strikes.addAll(newStrikes);
        }
    }

    @Getter
    @Setter
    public static class OptionContractData {
        private String symbol;
        private String expiry;
        private double strike;
        private String type;
        private double lastPrice;
        private double bidPrice;
        private double askPrice;
        private long volume;
        private long openInterest;
        private OptionGreeks greeks;
    }

    @Getter
    @Setter
    public static class OrderStatusData {
        private final int orderId;
        private final String symbol;
        private String status;
        private long filled;
        private long remaining;
        private double avgFillPrice;

        public OrderStatusData(int orderId, String symbol, String status) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.status = status;
        }
    }

    public static class IbkrApiException extends RuntimeException {
        private final int errorCode;

        public IbkrApiException(int errorCode, String message) {
            super("IBKR API Error " + errorCode + ": " + message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
}
