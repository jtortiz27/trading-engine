package com.trading.ibkr.service;

import com.ib.client.*;
import com.trading.ibkr.config.IbkrGatewayProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IbkrConnectionManager implements EWrapper {

  private final IbkrGatewayProperties properties;
  private final IbkrEventHandler eventHandler;

  private EClientSocket clientSocket;
  private EReaderSignal readerSignal;
  private EReader reader;

  private final AtomicBoolean connected = new AtomicBoolean(false);
  private final AtomicInteger nextRequestId = new AtomicInteger(1);
  private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

  @PostConstruct
  public void init() {
    readerSignal = new EJavaSignal();
    clientSocket = new EClientSocket(this);
    eventHandler.setClientSocket(clientSocket);
    connect();
  }

  @PreDestroy
  public void disconnect() {
    if (clientSocket != null && connected.get()) {
      log.info("Disconnecting from IB Gateway...");
      clientSocket.eDisconnect();
      connected.set(false);
    }
  }

  public void connect() {
    if (connected.get()) {
      return;
    }

    log.info(
        "Connecting to IB Gateway at {}:{}",
        properties.getHost(),
        properties.getPort());

    clientSocket.eConnect(properties.getHost(), properties.getPort(), properties.getClientId());

    // Start reader thread
    reader = new EReader(clientSocket, readerSignal);
    reader.start();

    // Start message processing thread
    new Thread(this::processMessages, "IBKR-Message-Processor").start();
  }

  public void reconnect() {
    log.warn("Attempting to reconnect to IB Gateway...");
    disconnect();
    try {
      Thread.sleep(properties.getReconnectDelayMs());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    connect();
  }

  private void processMessages() {
    while (clientSocket.isConnected()) {
      readerSignal.waitForSignal();
      try {
        reader.processMsgs();
      } catch (Exception e) {
        log.error("Error processing IBKR messages", e);
      }
    }
  }

  public int getNextRequestId() {
    return nextRequestId.getAndIncrement();
  }

  public boolean isConnected() {
    return connected.get();
  }

  public EClientSocket getClientSocket() {
    return clientSocket;
  }

  public CompletableFuture<Void> getConnectionFuture() {
    return connectionFuture;
  }

  // EWrapper Implementation

  @Override
  public void connected() {
    log.info("Connected to IB Gateway");
    connected.set(true);
    connectionFuture.complete(null);
  }

  @Override
  public void disconnected() {
    log.warn("Disconnected from IB Gateway");
    connected.set(false);
    connectionFuture.completeExceptionally(new RuntimeException("Disconnected from IB Gateway"));
  }

  @Override
  public void error(Exception e) {
    log.error("IB Gateway error", e);
  }

  @Override
  public void error(String str) {
    log.error("IB Gateway error: {}", str);
  }

  @Override
  public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
    log.error("IB Gateway error - ID: {}, Code: {}, Message: {}", id, errorCode, errorMsg);
    eventHandler.handleError(id, errorCode, errorMsg);
  }

  @Override
  public void connectionClosed() {
    log.warn("IB Gateway connection closed");
    connected.set(false);
    // Attempt reconnect if configured
    if (properties.getReconnectAttempts() > 0) {
      reconnect();
    }
  }

  @Override
  public void nextValidId(int orderId) {
    log.info("Next valid order ID: {}", orderId);
    nextRequestId.set(orderId);
  }

  // Market Data callbacks - delegated to event handler
  @Override
  public void tickPrice(int tickerId, int field, double price, int tickAttrib) {
    eventHandler.handleTickPrice(tickerId, field, price);
  }

  @Override
  public void tickSize(int tickerId, int field, Decimal size) {
    eventHandler.handleTickSize(tickerId, field, size.longValue());
  }

  @Override
  public void tickGeneric(int tickerId, int tickType, double value) {
    eventHandler.handleTickGeneric(tickerId, tickType, value);
  }

  @Override
  public void tickString(int tickerId, int tickType, String value) {
    eventHandler.handleTickString(tickerId, tickType, value);
  }

  // Options chain callbacks
  @Override
  public void securityDefinitionOptionalParameter(
      int reqId,
      String exchange,
      int underlyingConId,
      String tradingClass,
      String multiplier,
      Set<String> expirations,
      Set<Double> strikes) {
    eventHandler.handleSecurityDefinitionOptionalParameter(
        reqId, exchange, tradingClass, multiplier, expirations, strikes);
  }

  @Override
  public void securityDefinitionOptionalParameterEnd(int reqId) {
    eventHandler.handleSecurityDefinitionOptionalParameterEnd(reqId);
  }

  // Contract details for options
  @Override
  public void contractDetails(int reqId, ContractDetails contractDetails) {
    eventHandler.handleContractDetails(reqId, contractDetails);
  }

  @Override
  public void contractDetailsEnd(int reqId) {
    eventHandler.handleContractDetailsEnd(reqId);
  }

  // Order callbacks
  @Override
  public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
    eventHandler.handleOpenOrder(orderId, contract, order, orderState);
  }

  @Override
  public void orderStatus(
      int orderId,
      String status,
      Decimal filled,
      Decimal remaining,
      double avgFillPrice,
      int permId,
      int parentId,
      double lastFillPrice,
      int clientId,
      String whyHeld,
      double mktCapPrice) {
    eventHandler.handleOrderStatus(
        orderId, status, filled.longValue(), remaining.longValue(), avgFillPrice, lastFillPrice);
  }

  @Override
  public void execDetails(int reqId, Contract contract, Execution execution) {
    eventHandler.handleExecDetails(reqId, contract, execution);
  }

  @Override
  public void execDetailsEnd(int reqId) {
    eventHandler.handleExecDetailsEnd(reqId);
  }

  // Remaining EWrapper methods - no-ops or basic logging
  @Override
  public void tickOptionComputation(
      int tickerId,
      int field,
      int tickAttrib,
      double impliedVol,
      double delta,
      double optPrice,
      double pvDividend,
      double gamma,
      double vega,
      double theta,
      double undPrice) {
    eventHandler.handleTickOptionComputation(
        tickerId, field, impliedVol, delta, gamma, theta, vega, undPrice);
  }

  @Override
  public void accountSummary(int reqId, String account, String tag, String value, String currency) {
  }

  @Override
  public void accountSummaryEnd(int reqId) {
  }

  @Override
  public void verifyMessageAPI(String apiData) {
  }

  @Override
  public void verifyCompleted(boolean isSuccessful, String errorText) {
  }

  @Override
  public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {
  }

  @Override
  public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {
  }

  @Override
  public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {
  }

  @Override
  public void positionMultiEnd(int reqId) {
  }

  @Override
  public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {
  }

  @Override
  public void accountUpdateMultiEnd(int reqId) {
  }

  @Override
  public void tickSnapshotEnd(int reqId) {
    eventHandler.handleTickSnapshotEnd(reqId);
  }

  @Override
  public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
  }

  @Override
  public void updateAccountTime(String timeStamp) {
  }

  @Override
  public void accountDownloadEnd(String accountName) {
  }

  @Override
  public void updateMktDepth(int tickerId, int position, String operation, int side, double price, Decimal size) {
  }

  @Override
  public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {
  }

  @Override
  public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
  }

  @Override
  public void receiveFA(int faDataType, String xml) {
  }

  @Override
  public void scannerParameters(String xml) {
  }

  @Override
  public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
  }

  @Override
  public void scannerDataEnd(int reqId) {
  }

  @Override
  public void bondContractDetails(int reqId, ContractDetails contractDetails) {
  }

  @Override
  public void historicalData(int reqId, Bar bar) {
  }

  @Override
  public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
  }

  @Override
  public void historicalDataUpdate(int reqId, Bar bar) {
  }

  @Override
  public void rerouteMktDataReq(int reqId, int conId, String exchange) {
  }

  @Override
  public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
  }

  @Override
  public void fundamentalData(int reqId, String data) {
  }

  @Override
  public void updateAccountValue(String key, String value, String currency, String accountName) {
  }

  @Override
  public void position(String account, Contract contract, Decimal pos, double avgCost) {
  }

  @Override
  public void positionEnd() {
  }

  @Override
  public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
  }

  @Override
  public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
  }

  @Override
  public void openOrderEnd() {
  }

  @Override
  public void managedAccounts(String accountsList) {
  }

  @Override
  public void historicalTick(int reqId, long time, double price, Decimal size) {
  }

  @Override
  public void historicalTickBidAsk(int reqId, long time, int mask, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize) {
  }

  @Override
  public void historicalTickLast(int reqId, long time, int mask, double price, Decimal size, String exchange, String specialConditions) {
  }

  @Override
  public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
  }

  @Override
  public void currentTime(long time) {
  }

  @Override
  public void headTimestamp(int reqId, String headTimestamp) {
  }

  @Override
  public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
  }

  @Override
  public void commissionReport(CommissionReport commissionReport) {
  }

  @Override
  public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {
  }

  @Override
  public void marketDataType(int reqId, int marketDataType) {
  }

  @Override
  public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {
  }

  @Override
  public void newsProviders(NewsProvider[] newsProviders) {
  }

  @Override
  public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline, String extraData) {
  }

  @Override
  public void newsArticle(int requestId, int articleType, String articleText) {
  }

  @Override
  public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {
  }

  @Override
  public void historicalNewsEnd(int requestId, boolean hasMore) {
  }

  @Override
  public void histogramData(int reqId, List<HistogramEntry> items) {
  }

  @Override
  public void wshMetaData(int reqId, String dataJson) {
  }

  @Override
  public void wshEventData(int reqId, String dataJson) {
  }

  @Override
  public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {
  }

  @Override
  public void userInfo(int reqId, String whiteBrandingId) {
  }
}
