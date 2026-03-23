# IBKR Integration Guide

## Overview

This module provides production-ready integration with Interactive Brokers (IBKR) TWS/Gateway for the trading engine.

## Features

- **Real-time Market Data**: Bid/ask/last prices, volume, VIX
- **Options Chain Retrieval**: Full chains with Greeks
- **Order Placement**: Paper trading support first
- **Connection Management**: Auto-reconnect, health monitoring
- **Strategy Selection**: Iron Condor, Butterfly, Credit/Debit Spreads
- **Trade Recommendations**: Complete flow from market data to recommendation

## Architecture

```
IBKR Gateway (7497/7496)
       ↓
IbkrConnectionManager
       ↓
IbkrWrapper (callbacks)
       ↓
IbkrMarketDataServiceImpl ← implements IbkrMarketDataClient
       ↓
OptionsDataService / MarketDataService
       ↓
RecommendationService
       ↓
StrategyRecommendation + TradeRecommendation
```

## Configuration

### application.yml

```yaml
ibkr:
  host: localhost
  port: 7497          # 7497 = paper trading, 7496 = live
  client-id: 1
  mode: PAPER
  connection-timeout-seconds: 30
  market-data-timeout-seconds: 10
  options-chain-timeout-seconds: 30
  order-timeout-seconds: 10
  auto-reconnect: true
  reconnect-delay-seconds: 5
  max-reconnect-attempts: 0    # 0 = unlimited
```

### IB Gateway Setup

1. Download IB Gateway from: https://www.interactivebrokers.com/en/index.php?f=16457
2. Install and configure:
   - Enable "API" → "Settings" → "Enable ActiveX and Socket Clients"
   - Set Socket Port: 7497 (paper) or 7496 (live)
   - Check "Create API message log"
   - Check "Allow connections from localhost only" (for security)
3. Start IB Gateway and log in with paper trading credentials

### Enable Live Trading

```yaml
# application-live.yml
spring:
  profiles:
    active: ibkr-live

ibkr:
  port: 7496
  mode: LIVE
```

## API Endpoints

### Market Data

```bash
# Get market data for a symbol
GET /api/ibkr/market-data/{symbol}

# Example: AAPL
GET /api/ibkr/market-data/AAPL
```

### Options Chain

```bash
# Get full options chain
GET /api/ibkr/options-chain/{symbol}

# Get near-the-money only
GET /api/ibkr/options-chain/{symbol}/near-the-money

# Get condor-optimized expirations (20-45 DTE)
GET /api/ibkr/options-chain/{symbol}/condor-optimized
```

### Trade Recommendations

```bash
# Get recommendation for single symbol
GET /api/ibkr/recommendation/{symbol}

# Get recommendations for multiple symbols
GET /api/ibkr/recommendations?symbols=AAPL,TSLA,SPY
```

## Example Response

### Market Data

```json
{
  "symbol": "AAPL",
  "lastPrice": 175.50,
  "bidPrice": 175.45,
  "askPrice": 175.55,
  "volume": 45000000,
  "vix": 18.5,
  "timestamp": "2024-03-22T14:30:00Z"
}
```

### Options Chain

```json
{
  "underlying": "AAPL",
  "underlyingPrice": 175.50,
  "calls": [
    {
      "symbol": "AAPL240322C00175000",
      "expiry": "2024-03-22",
      "strike": 175.00,
      "type": "CALL",
      "lastPrice": 2.50,
      "bidPrice": 2.45,
      "askPrice": 2.55,
      "volume": 5000,
      "openInterest": 25000,
      "impliedVolatility": 0.25,
      "delta": 0.55,
      "gamma": 0.04,
      "theta": -0.15,
      "vega": 0.20,
      "rho": 0.03
    }
  ],
  "puts": [...],
  "timestamp": "2024-03-22"
}
```

### Trade Recommendation

```json
{
  "symbol": "AAPL",
  "underlyingPrice": 175.50,
  "strategy": {
    "strategyType": "IRON_CONDOR",
    "confidence": 85.0,
    "reasons": [
      "High IV percentile (75%) favors premium selling strategies",
      "Market sentiment is NEUTRAL",
      "Iron Condor profits from range-bound movement with defined risk"
    ],
    "ivPercentile": 75.0,
    "sentiment": "NEUTRAL"
  },
  "legs": [
    {
      "type": "SHORT_PUT",
      "strike": 166.73,
      "quantity": 1
    },
    {
      "type": "SHORT_CALL",
      "strike": 184.28,
      "quantity": 1
    }
  ],
  "positionSize": 9500.00,
  "expectedProfit": 1282.50,
  "maxRisk": 4750.00,
  "riskRewardRatio": 0.27,
  "ivPercentile": 75.0,
  "sentiment": "NEUTRAL",
  "timestamp": "2024-03-22"
}
```

## Testing

### Prerequisites

- IB Gateway running on localhost:7497
- Paper trading account configured

### Run Tests

```bash
./gradlew :ibkr-integration:test
```

### Manual Testing

Test with AAPL, TSLA, SPY first:

```bash
# Start IB Gateway
# Run the application
./gradlew :api-java-webflux:bootRun

# Test endpoints
curl http://localhost:8080/api/ibkr/market-data/AAPL
curl http://localhost:8080/api/ibkr/options-chain/SPY
curl http://localhost:8080/api/ibkr/recommendation/TSLA
```

## Troubleshooting

### Connection Issues

1. **"Not connected to IBKR Gateway"**
   - Verify IB Gateway is running
   - Check port settings (7497 paper, 7496 live)
   - Ensure "Enable ActiveX and Socket Clients" is checked

2. **"Connection refused"**
   - Verify IB Gateway is running on specified host/port
   - Check firewall settings
   - Try telnet: `telnet localhost 7497`

3. **Timeout errors**
   - Increase timeout in configuration
   - Check network connectivity
   - Verify market is open

### Common Error Codes

- **2104**: Market data farm connection OK (not an error)
- **2106**: HMDS data farm connection OK (not an error)
- **200**: No security definition found (invalid symbol)
- **354**: Requested market data is not subscribed

## Security Considerations

- Always use paper trading mode for development
- Enable "Allow connections from localhost only" in IB Gateway
- Store credentials securely (not in code)
- Use environment variables for sensitive configuration

## Future Enhancements

1. Historical options data
2. Real-time streaming via WebSocket
3. Position tracking
4. P&L calculation
5. Risk metrics (VaR, expected shortfall)

## References

- IBKR API Documentation: https://interactivebrokers.github.io/
- TWS API Guide: https://interactivebrokers.github.io/tws-api/
