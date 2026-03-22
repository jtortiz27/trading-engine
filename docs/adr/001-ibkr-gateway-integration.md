# ADR-001: Interactive Brokers (IBKR) Gateway Integration

## Status
Accepted - 2026-03-22

## Context

The trading engine currently relies on Massive and Polygon APIs for market data. We have encountered persistent issues with these providers:

- **Massive API**: Returning 403 errors consistently, making the service unusable
- **Polygon API**: Basic tier is insufficient for our requirements (no real-time VIX, limited options data)

We need a reliable market data source that provides:
- Real-time VIX data for volatility-based strategy decisions
- Full options chain data with Greeks
- Low-latency execution capability
- Paper trading environment for strategy validation

## Decision

Use **Interactive Brokers (IBKR) Gateway** as the primary market data source and execution platform.

### Key Points

1. **IBKR Gateway** will run locally and provide TWS API access
2. **Paper trading mode** will be the default (port 7497)
3. A new `ibkr-client` module will wrap the TWS Java API with reactive (Mono/Flux) interfaces
4. The existing `api-java-webflux` module will be refactored to use IBKR instead of Polygon

## Consequences

### Positive

- **Free market data** with a funded IBKR account
- **VIX data available** - critical for our CondorScore strategy
- **Full options chains** with implied volatility and Greeks
- **Built-in paper trading** for strategy validation
- **Industry-standard API** with extensive documentation
- **No rate limits** on data requests through TWS API
- **Real-time streaming** data via callbacks wrapped in Flux

### Negative

- **Requires IB Gateway running locally** - adds operational complexity
- **Single point of failure** - if Gateway is down, data flow stops
- **Learning curve** - TWS API is powerful but complex
- **Connection management** - requires heartbeat and reconnection logic

### Neutral

- **Java codebase** - TWS Java API fits our existing stack
- **Port 7497 default** - configurable for live trading (7496)

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        Architecture                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────┐      ┌──────────────────┐            │
│   │  WebFlux API    │─────▶│  Model Sidecar   │            │
│   │  (Port 8080)    │◀─────│  (Port 8081)     │            │
│   └────────┬────────┘      └──────────────────┘            │
│            │                                                │
│            │  Mono<TickData> / Flux<TickData>             │
│            ▼                                                │
│   ┌─────────────────┐      ┌──────────────────┐            │
│   │   ibkr-client   │─────▶│   IB Gateway     │            │
│   │   (NEW MODULE)  │      │   (Port 7497)    │            │
│   └─────────────────┘      │   Paper Trading  │            │
│                            └──────────────────┘            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## API Changes

### Current Flow (Polygon)
```
GET /recommendations/?symbol={symbol}
  └─▶ Polygon API for market data
  └─▶ Returns TickerSummaryResource
```

### New Flow (IBKR + Model Sidecar)
```
GET /recommendations/?symbol={symbol}
  ├─▶ POST /predict to model-sidecar with StockFeatures
  ├─▶ If confidence > threshold:
  │     ├─▶ IBKR.requestMarketData(symbol) → Flux<TickData>
  │     └─▶ IBKR.requestOptionsChain(symbol) → OptionsChain
  └─▶ Returns enriched recommendation with market data + options
```

## Configuration

```yaml
ibkr:
  gateway:
    host: localhost
    port: 7497          # 7497 = paper, 7496 = live
    client-id: 0        # Unique client ID
  paper-trading: true   # Safety flag

model-sidecar:
  url: http://localhost:8081
```

## Data Types

### MarketData (Stub)
```java
public class MarketData {
    private String symbol;
    private Double lastPrice;
    private Double bidPrice;
    private Double askPrice;
    private Long volume;
    private Double vix;
    private Instant timestamp;
}
```

### OptionsChain (Stub)
```java
public class OptionsChain {
    private String underlying;
    private List<OptionContract> calls;
    private List<OptionContract> puts;
    private Double underlyingPrice;
}
```

### OptionContract (Stub)
```java
public class OptionContract {
    private String symbol;
    private String expiry;
    private Double strike;
    private String type; // CALL or PUT
    private Double lastPrice;
    private Double impliedVolatility;
    private Double delta;
    private Double gamma;
    private Double theta;
    private Double vega;
}
```

## Migration Plan

1. ✅ Create ADR (this document)
2. ⬜ Create `ibkr-client` module with TWS API wrapper
3. ⬜ Refactor `api-java-webflux` to use IBKR instead of Polygon
4. ⬜ Add model-sidecar integration for ML predictions
5. ⬜ Update configuration and documentation
6. ⬜ Test paper trading flow
7. ⬜ Archive Polygon integration code

## References

- IBKR TWS API Docs: https://interactivebrokers.github.io/tws-api/
- Paper Trading: https://www.interactivebrokers.com/en/index.php?f=16457
- TWS Java API: https://interactivebrokers.github.io/tws-api/

## Decision Owner

- Jason Ortiz (@jtortiz27)
- Date: 2026-03-22
