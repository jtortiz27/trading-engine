package com.trading.options.analytics;

/**
 * Black-Scholes option pricing and Greeks calculation.
 * Used for computing implied volatility and verifying greeks.
 */
public class BlackScholes {

  private static final double D1_CUTOFF = 10.0;
  private static final double D2_CUTOFF = 10.0;

  /**
   * Standard normal cumulative distribution function.
   */
  public static double normCdf(double x) {
    if (x < -D1_CUTOFF) return 0.0;
    if (x > D1_CUTOFF) return 1.0;
    return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
  }

  /**
   * Standard normal probability density function.
   */
  public static double normPdf(double x) {
    return Math.exp(-0.5 * x * x) / Math.sqrt(2.0 * Math.PI);
  }

  /**
   * Error function approximation using Abramowitz and Stegun formula.
   */
  public static double erf(double x) {
    // Save the sign of x
    double sign = x < 0 ? -1.0 : 1.0;
    x = Math.abs(x);

    // Constants
    double a1 = 0.254829592;
    double a2 = -0.284496736;
    double a3 = 1.421413741;
    double a4 = -1.453152027;
    double a5 = 1.061405429;
    double p = 0.3275911;

    // A&S formula 7.1.26
    double t = 1.0 / (1.0 + p * x);
    double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

    return sign * y;
  }

  /**
   * Calculate d1 parameter for Black-Scholes.
   */
  public static double d1(double spot, double strike, double riskFreeRate,
                          double timeToExpiry, double volatility) {
    if (volatility <= 0 || timeToExpiry <= 0) return 0.0;
    return (Math.log(spot / strike) + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiry)
        / (volatility * Math.sqrt(timeToExpiry));
  }

  /**
   * Calculate d2 parameter for Black-Scholes.
   */
  public static double d2(double spot, double strike, double riskFreeRate,
                          double timeToExpiry, double volatility) {
    return d1(spot, strike, riskFreeRate, timeToExpiry, volatility)
        - volatility * Math.sqrt(timeToExpiry);
  }

  /**
   * Calculate call option price using Black-Scholes.
   */
  public static double callPrice(double spot, double strike, double riskFreeRate,
                                  double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0) {
      return Math.max(0.0, spot - strike);
    }
    if (volatility <= 0) {
      return Math.max(0.0, spot - strike);
    }

    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    double d2 = d2(spot, strike, riskFreeRate, timeToExpiry, volatility);

    return spot * normCdf(d1)
        - strike * Math.exp(-riskFreeRate * timeToExpiry) * normCdf(d2);
  }

  /**
   * Calculate put option price using Black-Scholes.
   */
  public static double putPrice(double spot, double strike, double riskFreeRate,
                               double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0) {
      return Math.max(0.0, strike - spot);
    }
    if (volatility <= 0) {
      return Math.max(0.0, strike - spot);
    }

    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    double d2 = d2(spot, strike, riskFreeRate, timeToExpiry, volatility);

    return strike * Math.exp(-riskFreeRate * timeToExpiry) * normCdf(-d2)
        - spot * normCdf(-d1);
  }

  /**
   * Calculate call option delta.
   */
  public static double callDelta(double spot, double strike, double riskFreeRate,
                                 double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) {
      return spot > strike ? 1.0 : 0.0;
    }
    return normCdf(d1(spot, strike, riskFreeRate, timeToExpiry, volatility));
  }

  /**
   * Calculate put option delta.
   */
  public static double putDelta(double spot, double strike, double riskFreeRate,
                                double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) {
      return spot < strike ? -1.0 : 0.0;
    }
    return normCdf(d1(spot, strike, riskFreeRate, timeToExpiry, volatility)) - 1.0;
  }

  /**
   * Calculate option gamma (same for calls and puts).
   */
  public static double gamma(double spot, double strike, double riskFreeRate,
                             double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) return 0.0;
    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    return normPdf(d1) / (spot * volatility * Math.sqrt(timeToExpiry));
  }

  /**
   * Calculate option vega (same for calls and puts).
   */
  public static double vega(double spot, double strike, double riskFreeRate,
                            double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) return 0.0;
    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    return spot * normPdf(d1) * Math.sqrt(timeToExpiry) / 100.0; // Divided by 100 for percentage IV
  }

  /**
   * Calculate call option theta.
   */
  public static double callTheta(double spot, double strike, double riskFreeRate,
                                 double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0) return 0.0;
    if (volatility <= 0) return 0.0;
    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    double d2 = d2(spot, strike, riskFreeRate, timeToExpiry, volatility);

    double term1 = -spot * normPdf(d1) * volatility / (2.0 * Math.sqrt(timeToExpiry));
    double term2 = riskFreeRate * strike * Math.exp(-riskFreeRate * timeToExpiry) * normCdf(d2);

    return (term1 - term2) / 365.0; // Per day
  }

  /**
   * Calculate put option theta.
   */
  public static double putTheta(double spot, double strike, double riskFreeRate,
                                double timeToExpiry, double volatility) {
    if (timeToExpiry <= 0) return 0.0;
    if (volatility <= 0) return 0.0;
    double d1 = d1(spot, strike, riskFreeRate, timeToExpiry, volatility);
    double d2 = d2(spot, strike, riskFreeRate, timeToExpiry, volatility);

    double term1 = -spot * normPdf(d1) * volatility / (2.0 * Math.sqrt(timeToExpiry));
    double term2 = riskFreeRate * strike * Math.exp(-riskFreeRate * timeToExpiry) * normCdf(-d2);

    return (term1 + term2) / 365.0; // Per day
  }

  /**
   * Calculate implied volatility using Newton-Raphson method.
   *
   * @param price Market price of the option
   * @param spot Current underlying price
   * @param strike Option strike price
   * @param riskFreeRate Risk-free rate
   * @param timeToExpiry Time to expiry in years
   * @param isCall true for call, false for put
   * @return implied volatility or null if not found
   */
  public static Double impliedVolatility(double price, double spot, double strike,
                                         double riskFreeRate, double timeToExpiry,
                                         boolean isCall) {
    if (timeToExpiry <= 0 || price <= 0) return null;

    double iv = 0.5; // Initial guess: 50%
    double tolerance = 1e-6;
    int maxIterations = 100;

    for (int i = 0; i < maxIterations; i++) {
      double theoreticalPrice = isCall
          ? callPrice(spot, strike, riskFreeRate, timeToExpiry, iv)
          : putPrice(spot, strike, riskFreeRate, timeToExpiry, iv);

      double diff = theoreticalPrice - price;
      if (Math.abs(diff) < tolerance) {
        return iv;
      }

      double vega = vega(spot, strike, riskFreeRate, timeToExpiry, iv) * 100.0; // Scale back
      if (Math.abs(vega) < 1e-10) {
        return null; // Vega too small, can't converge
      }

      iv = iv - diff / vega;

      // Bounds check
      if (iv < 0.001) iv = 0.001;
      if (iv > 5.0) iv = 5.0;
    }

    return iv; // Return best guess if max iterations reached
  }
}
