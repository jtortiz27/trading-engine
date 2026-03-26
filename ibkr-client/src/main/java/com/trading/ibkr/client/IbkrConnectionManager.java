package com.trading.ibkr.client;

import com.ib.client.*;
import com.trading.ibkr.config.IbkrGatewayProperties;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class IbkrConnectionManager implements EWrapper {

  private static final Logger log = LoggerFactory.getLogger(IbkrConnectionManager.class);

  private final IbkrGatewayProperties properties;
  private final EClientSocket clientSocket;
  private final Sinks.Many<Boolean> connectionSink = Sinks.many().multicast().directBestEffort();
  private volatile boolean isConnected = false;
  private volatile int serverVersion = 0;
  private volatile String connectionTime = null;

  private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

  public IbkrConnectionManager(IbkrGatewayProperties properties) {
    this.properties = properties;
    this.clientSocket = new EClientSocket(this);
  }

  /**
   * Connects to the IBKR Gateway asynchronously.
   *
   * @return Mono that completes when connection is established
   */
  public Mono<Void> connect() {
    return Mono.fromRunnable(() -> {
          log.info(
              "Connecting to IBKR Gateway at {}:{}", properties.getHost(), properties.getPort());
          clientSocket.eConnect(properties.getHost(), properties.getPort(), properties.getClientId());
        })
        .then(Mono.fromFuture(connectionFuture))
        .timeout(
            java.time.Duration.ofSeconds(properties.getConnectionTimeoutSeconds()),
            Mono.error(new RuntimeException("Connection to IBKR Gateway timed out")))
        .doOnSuccess(v -> log.info("Connected to IBKR Gateway"))
        .doOnError(e -> log.error("Failed to connect to IBKR Gateway", e));
  }

  /** Disconnects from the IBKR Gateway. */
  public void disconnect() {
    if (clientSocket.isConnected()) {
      log.info("Disconnecting from IBKR Gateway");
      clientSocket.eDisconnect();
      isConnected = false;
      connectionSink.tryEmitNext(false);
    }
  }

  /** Returns true if connected to the gateway. */
  public boolean isConnected() {
    return isConnected && clientSocket.isConnected();
  }

  /** Returns the underlying EClientSocket for direct API calls. */
  public EClientSocket getClientSocket() {
    return clientSocket;
  }

  /** Returns a reactive stream of connection status changes. */
  public reactor.core.publisher.Flux<Boolean> connectionStatus() {
    return connectionSink.asFlux();
  }

  // ==================== EWrapper Implementation ====================

  @Override
  public void connectAck(long timestamp) {
    log.debug("Connection acknowledged by IBKR at {}", timestamp);
  }

  @Override
  public void nextValidId(int orderId) {
    log.debug("Next valid order ID: {}", orderId);
    if (!connectionFuture.isDone()) {
      connectionFuture.complete(null);
    }
    isConnected = true;
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
    if (errorCode == 502) {
      // "Couldn't connect to TWS" - connection failed
      if (!connectionFuture.isDone()) {
        connectionFuture.completeExceptionally(
            new RuntimeException("Could not connect to IBKR Gateway: " + errorMsg));
      }
    }
    log.error("IBKR Error - ID: {}, Code: {}, Message: {}", id, errorCode, errorMsg);
  }

  @Override
  public void connectionClosed() {
    log.warn("Connection to IBKR Gateway closed");
    isConnected = false;
    connectionSink.tryEmitNext(false);
  }

  // ==================== Stub implementations for remaining EWrapper methods ====================
  // These will be implemented by specialized handlers for market data, options, etc.

  @Override
  public void tickPrice(int tickerId, int field, double price, TickAttrib attrib) {}

  @Override
  public void tickSize(int tickerId, int field, Decimal size) {}

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
      double undPrice) {}

  @Override
  public void tickGeneric(int tickerId, int tickType, double value) {}

  @Override
  public void tickString(int tickerId, int tickType, String value) {}

  @Override
  public void tickEFP(
      int tickerId,
      int tickType,
      double basisPoints,
      String formattedBasisPoints,
      double impliedFuture,
      int holdDays,
      String futureLastTradeDate,
      double dividendImpact,
      double dividendsToLastTradeDate) {}

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
      double mktCapPrice) {}

  @Override
  public void openOrder(
      int orderId,
      Contract contract,
      Order order,
      OrderState orderState) {}

  @Override
  public void openOrderEnd() {}

  @Override
  public void updateAccountValue(
      String key, String value, String currency, String accountName) {}

  @Override
  public void updatePortfolio(
      Contract contract,
      Decimal position,
      double marketPrice,
      double marketValue,
      double averageCost,
      double unrealizedPNL,
      double realizedPNL,
      String accountName) {}

  @Override
  public void updateAccountTime(String timeStamp) {}

  @Override
  public void accountDownloadEnd(String accountName) {}

  @Override
  public void contractDetails(int reqId, ContractDetails contractDetails) {}

  @Override
  public void bondContractDetails(int reqId, ContractDetails contractDetails) {}

  @Override
  public void contractDetailsEnd(int reqId) {}

  @Override
  public void execDetails(int reqId, Contract contract, Execution execution) {}

  @Override
  public void execDetailsEnd(int reqId) {}

  @Override
  public void updateMktDepth(
      int tickerId,
      int position,
      int operation,
      int side,
      double price,
      Decimal size) {}

  @Override
  public void updateMktDepthL2(
      int tickerId,
      int position,
      String marketMaker,
      int operation,
      int side,
      double price,
      Decimal size,
      boolean isSmartDepth) {}

  @Override
  public void updateNewsBulletin(
      int msgId, int msgType, String message, String origExchange) {}

  @Override
  public void managedAccounts(String accountsList) {}

  @Override
  public void receiveFA(int faDataType, String xml) {}

  @Override
  public void historicalData(int reqId, Bar bar) {}

  @Override
  public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {}

  @Override
  public void scannerParameters(String xml) {}

  @Override
  public void scannerData(
      int reqId,
      int rank,
      ContractDetails contractDetails,
      String distance,
      String benchmark,
      String projection,
      String legsStr) {}

  @Override
  public void scannerDataEnd(int reqId) {}

  @Override
  public void realtimeBar(
      int reqId,
      long time,
      double open,
      double high,
      double low,
      double close,
      Decimal volume,
      Decimal wap,
      int count) {}

  @Override
  public void currentTime(long time) {}

  @Override
  public void fundamentalData(int reqId, String data) {}

  @Override
  public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {}

  @Override
  public void tickSnapshotEnd(int reqId) {}

  @Override
  public void position(String account, Contract contract, Decimal pos, double avgCost) {}

  @Override
  public void positionEnd() {}

  @Override
  public void stopRequested() {}

  @Override
  public void accountSummary(
      int reqId, String account, String tag, String value, String currency) {}

  @Override
  public void accountSummaryEnd(int reqId) {}

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
  public void positionMulti(
      int reqId,
      String account,
      String modelCode,
      Contract contract,
      Decimal pos,
      double avgCost) {}

  @Override
  public void positionMultiEnd(int reqId) {}

  @Override
  public void accountUpdateMulti(
      int reqId,
      String account,
      String modelCode,
      String key,
      String value,
      String currency) {}

  @Override
  public void accountUpdateMultiEnd(int reqId) {}

  @Override
  public void securityDefinitionOptionalParameter(
      int reqId,
      String exchange,
      int underlyingConId,
      String tradingClass,
      String multiplier,
      Set<String> expirations,
      Set<Double> strikes) {}

  @Override
  public void securityDefinitionOptionalParameterEnd(int reqId) {}

  @Override
  public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {}

  @Override
  public void familyCodes(FamilyCode[] familyCodes) {}

  @Override
  public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {}

  @Override
  public void historicalDataUpdate(int reqId, Bar bar) {}

  @Override
  public void rerouteMktDataReq(int reqId, int conId, String exchange) {}

  @Override
  public void rerouteMktDepthReq(int reqId, int conId, String exchange) {}

  @Override
  public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {}

  @Override
  public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}

  @Override
  public void pnlSingle(
      int reqId,
      Decimal pos,
      double dailyPnL,
      double unrealizedPnL,
      double realizedPnL,
      double value) {}

  @Override
  public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean done) {}

  @Override
  public void historicalTicksBidAsk(
      int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {}

  @Override
  public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {}

  @Override
  public void tickByTickAllLast(
      int reqId,
      int tickType,
      long time,
      double price,
      Decimal size,
      TickAttribLast tickAttribLast,
      String exchange,
      String specialConditions) {}

  @Override
  public void tickByTickBidAsk(
      int reqId,
      long time,
      double bidPrice,
      double askPrice,
      Decimal bidSize,
      Decimal askSize,
      TickAttribBidAsk tickAttribBidAsk) {}

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
  public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {}

  @Override
  public void userInfo(int reqId, String whiteBrandingId) {}
}
