package com.trading.options.analytics;

import static org.junit.jupiter.api.Assertions.*;

import com.trading.options.analytics.GammaDeltaFlowCalculator.GexResult;
import com.trading.options.analytics.GammaDeltaFlowCalculator.StrikeGex;
import com.trading.options.analytics.IronFlyCalculator.FlyContext;
import com.trading.options.analytics.IronFlyCalculator.FlyResult;
import com.trading.options.analytics.IronFlyCalculator.FlyStructure;
import com.trading.options.analytics.LiquidityCalculator.LiquidityResult;
import com.trading.options.analytics.OpenInterestCalculator.OiResult;
import com.trading.options.analytics.RealizedVolCalculator.GapStats;
import com.trading.options.analytics.RealizedVolCalculator.RealizedVolResult;
import com.trading.options.analytics.RealizedVolCalculator.VolRegime;
import com.trading.options.analytics.VolSurfaceCalculator.VolSurfaceResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Options Edge calculations.
 */
class OptionsAnalyticsTest {

  private static final double SPOT = 450.0;
  private static final double STRIKE_STEP = 5.0;
  private static final double BASE_IV = 0.20;
  private static final double DAYS_TO_EXPIRY = 30.0;

  private List<OptionStrikeData> testStrikes;

  @BeforeEach
  void setUp() {
    testStrikes = createTestStrikes();
  }

  /**
   * Create test strike data around SPY-like levels.
   */
  private List<OptionStrikeData> createTestStrikes() {
    List<OptionStrikeData> strikes = new ArrayList<>();

    // Create strikes from 400 to 500
    for (double strike = 400; strike <= 500; strike += STRIKE_STEP) {
      double distanceFromAtm = Math.abs(strike - SPOT);
      double moneyness = strike / SPOT;

      // Simplified Greeks calculation for testing
      double callDelta = approximateCallDelta(moneyness, BASE_IV, DAYS_TO_EXPIRY);
      double putDelta = callDelta - 1.0;
      double gamma = approximateGamma(moneyness, BASE_IV, DAYS_TO_EXPIRY);
      double theta = -0.05 - distanceFromAtm * 0.001;
      double vega = 0.1 + (1 - moneyness) * 0.05;

      // Higher OI near ATM
      long baseOi = (long) (10000 / (1 + distanceFromAtm / 10));
      long callOi = baseOi + (long) (Math.random() * 5000);
      long putOi = baseOi + (long) (Math.random() * 5000);

      // Higher volume near ATM
      long callVol = baseOi / 10;
      long putVol = baseOi / 10;

      OptionStrikeData strikeData = OptionStrikeData.builder()
          .strike(strike)
          .expiry(LocalDate.now().plusDays((int) DAYS_TO_EXPIRY))
          .daysToExpiry(DAYS_TO_EXPIRY)
          // Call data
          .callBid(Math.max(0.01, (SPOT - strike) * 0.5 + 5))
          .callAsk(Math.max(0.02, (SPOT - strike) * 0.5 + 5.1))
          .callLast((SPOT - strike) * 0.5 + 5.05)
          .callVolume(callVol)
          .callOpenInterest(callOi)
          .callIv(BASE_IV + (1 - moneyness) * 0.05)
          .callDelta(callDelta)
          .callGamma(gamma)
          .callTheta(theta)
          .callVega(vega)
          // Put data
          .putBid(Math.max(0.01, (strike - SPOT) * 0.5 + 5))
          .putAsk(Math.max(0.02, (strike - SPOT) * 0.5 + 5.1))
          .putLast((strike - SPOT) * 0.5 + 5.05)
          .putVolume(putVol)
          .putOpenInterest(putOi)
          .putIv(BASE_IV + (moneyness - 1) * 0.05)
          .putDelta(putDelta)
          .putGamma(gamma)
          .putTheta(theta)
          .putVega(vega)
          .build();

      strikes.add(strikeData);
    }

    return strikes;
  }

  /**
   * Approximate call delta using Black-Scholes-like formula for testing.
   */
  private double approximateCallDelta(double moneyness, double iv, double dte) {
    double d1 = (Math.log(moneyness) + 0.5 * iv * iv * dte / 365) / (iv * Math.sqrt(dte / 365));
    return 0.5 * (1 + Math.tanh(d1 / 2)); // Approximation of N(d1)
  }

  /**
   * Approximate gamma for testing.
   */
  private double approximateGamma(double moneyness, double iv, double dte) {
    return 0.01 / (1 + Math.abs(Math.log(moneyness)));
  }

  // ============================================================================
  // Black-Scholes Tests
  // ============================================================================

  @Test
  void testBlackScholesCallPrice() {
    double price = BlackScholes.callPrice(100, 100, 0.05, 1.0, 0.2);
    assertTrue(price > 0);
    assertTrue(price < 20); // ATM call should be reasonable
  }

  @Test
  void testBlackScholesPutCallParity() {
    double S = 100, K = 100, r = 0.05, T = 1.0, sigma = 0.2;
    double call = BlackScholes.callPrice(S, K, r, T, sigma);
    double put = BlackScholes.putPrice(S, K, r, T, sigma);
    double parity = call - put - S + K * Math.exp(-r * T);
    assertEquals(0, parity, 0.01);
  }

  @Test
  void testImpliedVolatility() {
    double S = 100, K = 100, r = 0.05, T = 1.0, sigma = 0.2;
    double marketPrice = BlackScholes.callPrice(S, K, r, T, sigma);

    Double iv = BlackScholes.impliedVolatility(marketPrice, S, K, r, T, true);
    assertNotNull(iv);
    assertEquals(sigma, iv, 0.001);
  }

  // ============================================================================
  // Gamma/Delta Flow Tests
  // ============================================================================

  @Test
  void testGexCalculation() {
    GexResult result = GammaDeltaFlowCalculator.calculateGex(testStrikes, SPOT);

    assertNotNull(result);
    assertNotNull(result.getTotalGex());
    assertTrue(result.getTotalGex() > 0);
    assertNotNull(result.getMaxGammaStrike());
    assertFalse(result.getStrikeGexData().isEmpty());
  }

  @Test
  void testDeltaWeightedGex() {
    OptionStrikeData strike = testStrikes.get(10); // Near ATM
    Double dgex = strike.calculateDeltaWeightedGex(SPOT);

    assertNotNull(dgex);
    assertTrue(dgex >= 0);
  }

  @Test
  void testZeroGammaFlip() {
    GexResult result = GammaDeltaFlowCalculator.calculateGex(testStrikes, SPOT);

    // Zero gamma flip might not exist in test data, but shouldn't crash
    if (result.getZeroGammaFlipLevel() != null) {
      assertTrue(result.getZeroGammaFlipLevel() > 0);
    }
  }

  @Test
  void testNetAtmGammaTilt() {
    GexResult result = GammaDeltaFlowCalculator.calculateGex(testStrikes, SPOT);

    assertNotNull(result.getNetAtmGammaTilt());
  }

  // ============================================================================
  // Open Interest Tests
  // ============================================================================

  @Test
  void testOiStructureCalculation() {
    OiResult result = OpenInterestCalculator.calculateOiStructure(testStrikes, SPOT, null);

    assertNotNull(result);
    assertNotNull(result.getMaxOiStrike());
    assertNotNull(result.getMaxOiAmount());
    assertTrue(result.getMaxOiAmount() > 0);
  }

  @Test
  void testOiWalls() {
    OiResult result = OpenInterestCalculator.calculateOiStructure(testStrikes, SPOT, null);

    assertNotNull(result.getOiWalls());
    // May or may not have walls in test data
  }

  @Test
  void testPinRiskIndex() {
    OiResult result = OpenInterestCalculator.calculateOiStructure(testStrikes, SPOT, null);

    assertNotNull(result.getPinRiskIndex());
    assertTrue(result.getPinRiskIndex() >= 0 && result.getPinRiskIndex() <= 1);
  }

  @Test
  void testPutCallRatio() {
    OiResult result = OpenInterestCalculator.calculateOiStructure(testStrikes, SPOT, null);

    assertNotNull(result.getPutCallRatio());
    assertTrue(result.getPutCallRatio() > 0);
  }

  // ============================================================================
  // Vol Surface Tests
  // ============================================================================

  @Test
  void testVolSurfaceCalculation() {
    List<VolSurfaceCalculator.VolPoint> points = createTestVolPoints();
    VolSurfaceResult result = VolSurfaceCalculator.calculateVolSurface(
        points, SPOT, SPOT, null);

    assertNotNull(result);
    assertNotNull(result.getAtmIv());
  }

  private List<VolSurfaceCalculator.VolPoint> createTestVolPoints() {
    List<VolSurfaceCalculator.VolPoint> points = new ArrayList<>();

    // Create vol points at various deltas
    for (double delta = 0.1; delta <= 0.5; delta += 0.05) {
      double iv = BASE_IV + (0.5 - delta) * 0.1; // Skew: lower delta = higher IV
      points.add(VolSurfaceCalculator.VolPoint.builder()
          .strike(SPOT * (1 + (delta - 0.5) * 0.2))
          .delta(delta)
          .iv(iv)
          .isCall(true)
          .build());

      points.add(VolSurfaceCalculator.VolPoint.builder()
          .strike(SPOT * (1 + (delta - 0.5) * 0.2))
          .delta(-delta)
          .iv(iv + 0.02) // Put skew
          .isCall(false)
          .build());
    }

    return points;
  }

  @Test
  void testRiskReversalCalculation() {
    List<VolSurfaceCalculator.VolPoint> points = createTestVolPoints();
    VolSurfaceResult result = VolSurfaceCalculator.calculateVolSurface(
        points, SPOT, SPOT, null);

    assertNotNull(result.getRr25());
  }

  @Test
  void testButterflyCalculation() {
    List<VolSurfaceCalculator.VolPoint> points = createTestVolPoints();
    VolSurfaceResult result = VolSurfaceCalculator.calculateVolSurface(
        points, SPOT, SPOT, null);

    assertNotNull(result.getBf25());
  }

  // ============================================================================
  // Realized Vol Tests
  // ============================================================================

  @Test
  void testHistoricalVolCalculation() {
    // Generate synthetic price history with known volatility
    List<Double> closes = new ArrayList<>();
    double price = 100;
    closes.add(price);

    for (int i = 0; i < 30; i++) {
      // Random walk with ~20% vol
      double dailyVol = 0.20 / Math.sqrt(252);
      price = price * (1 + (Math.random() - 0.5) * dailyVol);
      closes.add(price);
    }

    RealizedVolResult result = RealizedVolCalculator.calculateRealizedVol(
        closes, BASE_IV, null);

    assertNotNull(result);
    assertNotNull(result.getHv20());
    assertTrue(result.getHv20() > 0);
  }

  @Test
  void testVrpCalculation() {
    List<Double> closes = Arrays.asList(
        100.0, 101.0, 99.5, 100.5, 102.0, 101.5, 100.0, 99.0, 100.5, 101.0,
        100.0, 98.5, 99.5, 100.5, 101.5, 100.0, 99.0, 100.0, 101.0, 102.0
    );

    RealizedVolResult result = RealizedVolCalculator.calculateRealizedVol(
        closes, BASE_IV, null);

    assertNotNull(result.getVrpAtm());
    assertNotNull(result.getVrpRatio());
  }

  @Test
  void testVolRegime() {
    List<Double> closes = new ArrayList<>();
    double price = 100;
    for (int i = 0; i < 30; i++) {
      closes.add(price);
      price = price * (1 + (Math.random() - 0.5) * 0.001); // Low vol
    }

    RealizedVolResult result = RealizedVolCalculator.calculateRealizedVol(
        closes, 0.15, null);

    assertNotNull(result.getVolRegime());
    assertTrue(result.getVolRegime() == VolRegime.LOW ||
        result.getVolRegime() == VolRegime.NORMAL);
  }

  // ============================================================================
  // Liquidity Tests
  // ============================================================================

  @Test
  void testLiquidityCalculation() {
    LiquidityResult result = LiquidityCalculator.calculateLiquidity(testStrikes, SPOT);

    assertNotNull(result);
    assertNotNull(result.getMedianSpreadPct());
    assertNotNull(result.getMaxSpreadPct());
    assertNotNull(result.getLiquidityRating());
  }

  @Test
  void testLiquidityScore() {
    LiquidityResult result = LiquidityCalculator.calculateLiquidity(testStrikes, SPOT);

    assertNotNull(result.getLiquidityScore());
    assertTrue(result.getLiquidityScore() >= 0 && result.getLiquidityScore() <= 100);
  }

  // ============================================================================
  // Iron Fly Tests
  // ============================================================================

  @Test
  void testIronFlyConstruction() {
    FlyContext context = FlyContext.builder()
        .spot(SPOT)
        .forwardPrice(SPOT)
        .daysToExpiry(DAYS_TO_EXPIRY)
        .atmIv(BASE_IV)
        .build();

    FlyResult result = IronFlyCalculator.calculateIronFly(testStrikes, context);

    assertNotNull(result);
    assertNotNull(result.getStructure());
    assertNotNull(result.getFlyConviction());
  }

  @Test
  void testFlyStructureComponents() {
    FlyContext context = FlyContext.builder()
        .spot(SPOT)
        .forwardPrice(SPOT)
        .daysToExpiry(DAYS_TO_EXPIRY)
        .atmIv(BASE_IV)
        .build();

    FlyResult result = IronFlyCalculator.calculateIronFly(testStrikes, context);

    if (result.getStructure() != null) {
      FlyStructure struct = result.getStructure();
      assertNotNull(struct.getShortCallStrike());
      assertNotNull(struct.getShortPutStrike());
      assertNotNull(struct.getLongCallStrike());
      assertNotNull(struct.getLongPutStrike());
      assertNotNull(struct.getTotalCredit());
      assertTrue(struct.getLongCallStrike() > struct.getShortCallStrike());
      assertTrue(struct.getShortPutStrike() > struct.getLongPutStrike());
    }
  }

  @Test
  void testEdgeMetrics() {
    FlyContext context = FlyContext.builder()
        .spot(SPOT)
        .forwardPrice(SPOT)
        .daysToExpiry(DAYS_TO_EXPIRY)
        .atmIv(BASE_IV)
        .build();

    FlyResult result = IronFlyCalculator.calculateIronFly(testStrikes, context);

    assertNotNull(result.getFlyCenterScore());
    assertNotNull(result.getWingRichnessScore());
    assertNotNull(result.getThetaToMove());
    assertNotNull(result.getCpr());
    assertNotNull(result.getMrb());
  }

  @Test
  void testUrgencyRating() {
    FlyContext context = FlyContext.builder()
        .spot(SPOT)
        .forwardPrice(SPOT)
        .daysToExpiry(DAYS_TO_EXPIRY)
        .atmIv(BASE_IV)
        .build();

    FlyResult result = IronFlyCalculator.calculateIronFly(testStrikes, context);

    assertNotNull(result.getUrgencyRating());
    assertTrue(
        result.getUrgencyRating().equals("HIGH") ||
            result.getUrgencyRating().equals("MEDIUM") ||
            result.getUrgencyRating().equals("LOW") ||
            result.getUrgencyRating().equals("NO_TRADE")
    );
  }

  // ============================================================================
  // Service Integration Tests
  // ============================================================================

  @Test
  void testAnalyticsService() {
    OptionsAnalyticsService service = new OptionsAnalyticsService();

    List<Double> priceHistory = new ArrayList<>();
    double price = SPOT;
    for (int i = 0; i < 30; i++) {
      priceHistory.add(price);
      price = price * (1 + (Math.random() - 0.5) * 0.01);
    }

    OptionsAnalyticsService.AnalyticsResult result = service.analyzeOptionsChain(
        "SPY", SPOT, testStrikes, "2024-04-19", priceHistory, null);

    assertNotNull(result);
    assertEquals("SPY", result.getUnderlying());
    assertNotNull(result.getGexResult());
    assertNotNull(result.getOiResult());
    assertNotNull(result.getVolSurface());
    assertNotNull(result.getLiquidity());
    assertNotNull(result.getOverallSignal());
  }

  @Test
  void testEmptyStrikes() {
    GexResult gex = GammaDeltaFlowCalculator.calculateGex(new ArrayList<>(), SPOT);
    assertNotNull(gex);

    OiResult oi = OpenInterestCalculator.calculateOiStructure(new ArrayList<>(), SPOT, null);
    assertNotNull(oi);

    LiquidityResult liq = LiquidityCalculator.calculateLiquidity(new ArrayList<>(), SPOT);
    assertTrue(liq.isThin());
  }

  @Test
  void testDeltaWeightedGexFormula() {
    // Test the specific formula: Gamma * abs(Delta) * S / 100
    OptionStrikeData strike = OptionStrikeData.builder()
        .strike(450.0)
        .callGamma(0.02)
        .callDelta(0.5)
        .callOpenInterest(1000L)
        .putGamma(0.02)
        .putDelta(-0.5)
        .putOpenInterest(1000L)
        .build();

    Double dgex = strike.calculateDeltaWeightedGex(SPOT);

    // Manual calculation
    double expectedCall = 0.02 * 0.5 * SPOT / 100.0 * 1000;
    double expectedPut = 0.02 * 0.5 * SPOT / 100.0 * 1000;
    double expected = expectedCall + expectedPut;

    assertEquals(expected, dgex, 0.001);
  }

  @Test
  void testRr25Calculation() {
    // RR25 = IV(25d call) - IV(25d put)
    double iv25Call = 0.22;
    double iv25Put = 0.25;

    double rr25 = iv25Call - iv25Put;
    assertEquals(-0.03, rr25, 0.001);
    assertTrue(rr25 < 0); // Negative = put skew (bearish)
  }

  @Test
  void testBf25Calculation() {
    // BF25 = 0.5*(IV(25d call)+IV(25d put)) - IV(ATM)
    double iv25Call = 0.22;
    double iv25Put = 0.25;
    double ivAtm = 0.20;

    double bf25 = 0.5 * (iv25Call + iv25Put) - ivAtm;
    assertEquals(0.135 - 0.20, bf25, 0.001);
  }
}
