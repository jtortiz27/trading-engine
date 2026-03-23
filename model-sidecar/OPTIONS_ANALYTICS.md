# Options Analytics Module

Ported from Options Edge Agent v3.1a - All calculations implemented in `model-sidecar` module.

## Structure

```
model-sidecar/src/main/java/com/trading/options/analytics/
├── OptionStrikeData.java          # Core data structure for option strikes
├── BlackScholes.java              # Black-Scholes pricing and Greeks
├── GammaDeltaFlowCalculator.java  # GEX, Delta-weighted GEX, Zero-gamma flip
├── OpenInterestCalculator.java    # OI Walls, Pin Risk, Momentum
├── VolSurfaceCalculator.java      # RR25, BF25, Term slope, Wing richness
├── RealizedVolCalculator.java     # HV10/HV20, VRP, Gap stats
├── LiquidityCalculator.java       # Spread metrics, Thin flag
├── IronFlyCalculator.java         # Fly construction, edge metrics
└── OptionsAnalyticsService.java   # Service layer, trade ranking
```

## Implemented Calculations

### 1. Gamma & Delta Flow (GammaDeltaFlowCalculator)
- **Delta-weighted GEX**: `Gamma * abs(Delta) * Spot / 100 * OI`
- **Zero-gamma flip levels**: Where cumulative Delta-GEX crosses zero
- **Net-ATM Gamma Tilt**: Gamma imbalance above vs below spot
- **Max Gamma Strike**: Primary price magnet

### 2. Open Interest Structure (OpenInterestCalculator)
- **OI Walls**: Strikes with OI ≥ 5x rolling 5-strike SMA
- **Pin-Risk Index**: 0-1 scale for pin probability
- **Wall Distance**: $ and % from spot
- **Put/Call Ratio**: Aggregate sentiment
- **OI Momentum**: Day-over-day % change (supports historical data)

### 3. Vol-Surface Shape (VolSurfaceCalculator)
- **RR25**: `IV(25d call) - IV(25d put)` - Risk reversal
- **BF25**: `0.5*(IV(25d call)+IV(25d put)) - IV(ATM)` - Butterfly
- **Term Slope**: `IV(~30D) - IV(~60D)`
- **Wing Richness Z-Score**: Based on historical BF25 distribution

### 4. Realized vs Implied (RealizedVolCalculator)
- **HV10/HV20**: Close-to-close stdev * sqrt(252)
- **VRP_ATM**: `IV_ATM² - HV20²`
- **VRP Ratio**: `IV_ATM / HV20`
- **Gap Statistics**: Median, 90th percentile, max up/down
- **Vol Regime**: LOW/NORMAL/ELEVATED/EXTREME

### 5. Liquidity / Quote Quality (LiquidityCalculator)
- **Spread Metrics**: Median/Max bid-ask width % across top-10 OI strikes
- **Thin Flag**: Based on spread thresholds and volume/OI minimums
- **Liquidity Score**: 0-100 composite
- **Rating**: EXCELLENT/GOOD/FAIR/POOR/THIN

### 6. Iron Fly Edge Metrics (IronFlyCalculator)
- **FlyCenterScore**: PinIndex + proximity + Gamma Tilt + OI surge
- **WingRichnessScore**: Based on BF25 (lower = better for buyers)
- **Theta-to-Move (T/M)**: `Daily Theta / Expected Daily Move`
- **BEPG**: Breach probability estimate
- **CpR**: Credit-per-Risk ratio
- **MRB**: Magnet-Risk Balance

### 7. Fly Construction Logic
- **Center Selection**: Near forward price (ATM)
- **Wing-Widen Loop**: Max 3 iterations, 1.5x widening each time
- **Fallback**: Iron Condor with OTM short strikes

### 8. Ranking & Triage
- **FlyConviction**: Weighted composite of all edge metrics
- **Urgency**: HIGH/MEDIUM/LOW/NO_TRADE based on conviction threshold

## Black-Scholes Implementation

```java
// Pricing
double callPrice = BlackScholes.callPrice(S, K, r, T, sigma);
double putPrice = BlackScholes.putPrice(S, K, r, T, sigma);

// Greeks
double delta = BlackScholes.callDelta(S, K, r, T, sigma);
double gamma = BlackScholes.gamma(S, K, r, T, sigma);
double theta = BlackScholes.callTheta(S, K, r, T, sigma);
double vega = BlackScholes.vega(S, K, r, T, sigma);

// Implied Volatility
Double iv = BlackScholes.impliedVolatility(price, S, K, r, T, isCall);
```

## Usage Example

```java
OptionsAnalyticsService service = new OptionsAnalyticsService();

AnalyticsResult result = service.analyzeOptionsChain(
    "SPY",                    // underlying
    450.0,                    // spot price
    strikes,                  // List<OptionStrikeData>
    "2024-04-19",            // expiry
    priceHistory,            // List<Double> closes
    gaps                     // List<Double> overnight gaps
);

// Access results
GexResult gex = result.getGexResult();
OiResult oi = result.getOiResult();
FlyResult fly = result.getFlyResult();
List<TradeIdea> ideas = result.getRankedTradeIdeas();
```

## Data Flow

1. **Input**: Options chain data (strikes, expiries, OI, IV, bid/ask, Greeks)
2. **Process**: Calculate all component analytics in parallel-friendly structure
3. **Construct**: Iron Fly/Condor based on market conditions
4. **Score**: Apply edge metrics to generate conviction
5. **Output**: FlyScores, priority queue, trade ideas

## Testing

Unit tests cover:
- Black-Scholes put-call parity and IV convergence
- GEX calculations with Delta-weighted formula
- OI Wall detection and Pin Risk calculation
- Vol surface metrics (RR25, BF25)
- Historical volatility and VRP
- Liquidity scoring
- Iron Fly construction and edge metrics
- Service integration

Run tests:
```bash
./gradlew :model-sidecar:test
```

## Notes

- All calculations use `double` for precision
- Deterministic: no randomness in core calculations
- Thread-safe: stateless calculators
- Null-safe: returns null or empty results for invalid inputs
- Supports both single expiry and term structure analysis
