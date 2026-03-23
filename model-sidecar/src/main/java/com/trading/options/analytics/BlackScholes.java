package com.trading.options.analytics;

/**
 * Black-Scholes option pricing and Greeks calculation.
 * Used for computing implied volatility and verifying greeks.
 */
public class BlackScholes {

  // Cutoff for d1/d2 values beyond which normal CDF approximates to 0 or 1
  // Prevents numerical instability for extreme values
  private static final double STANDARD_NORMAL_CUTOFF = 10.0;

  /**
   * Standard normal cumulative distribution function.
   * Returns the probability that a normally distributed random variable
   * with mean 0 and standard deviation 1 is less than or equal to the given value.
   *
   * @param normalizedValue The value to evaluate (typically d1 or d2 from Black-Scholes)
   * @return The cumulative probability, between 0 and 1
   */
  public static double calculateNormalCumulativeDistribution(double normalizedValue) {
    if (normalizedValue < -STANDARD_NORMAL_CUTOFF) return 0.0;
    if (normalizedValue > STANDARD_NORMAL_CUTOFF) return 1.0;
    return 0.5 * (1.0 + erf(normalizedValue / Math.sqrt(2.0)));
  }

  /**
   * Standard normal probability density function.
   *
   * @param normalizedValue The value to evaluate
   * @return The probability density at the given value
   */
  public static double calculateNormalProbabilityDensity(double normalizedValue) {
    return Math.exp(-0.5 * normalizedValue * normalizedValue) / Math.sqrt(2.0 * Math.PI);
  }

  /**
   * Error function approximation using Abramowitz and Stegun formula.
   * Used as part of the normal CDF calculation.
   *
   * @param value The input value
   * @return The error function approximation
   */
  public static double erf(double value) {
    // Save the sign of input value
    double sign = value < 0 ? -1.0 : 1.0;
    double absoluteValue = Math.abs(value);

    // Constants for Abramowitz and Stegun formula 7.1.26
    double coefficientA1 = 0.254829592;
    double coefficientA2 = -0.284496736;
    double coefficientA3 = 1.421413741;
    double coefficientA4 = -1.453152027;
    double coefficientA5 = 1.061405429;
    double coefficientP = 0.3275911;

    // A&S formula 7.1.26
    double t = 1.0 / (1.0 + coefficientP * absoluteValue);
    double approximation = 1.0 - (((((coefficientA5 * t + coefficientA4) * t) + coefficientA3) * t
        + coefficientA2) * t + coefficientA1) * t * Math.exp(-absoluteValue * absoluteValue);

    return sign * approximation;
  }

  /**
   * Calculate d1 parameter for Black-Scholes.
   * d1 = (ln(S/K) + (r + σ²/2)T) / (σ√T)
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The d1 parameter
   */
  public static double calculateD1(double underlyingSpotPrice, double strikePrice,
                                    double riskFreeRate, double timeToExpiry, double volatility) {
    if (volatility <= 0 || timeToExpiry <= 0) return 0.0;
    return (Math.log(underlyingSpotPrice / strikePrice)
        + (riskFreeRate + 0.5 * volatility * volatility) * timeToExpiry)
        / (volatility * Math.sqrt(timeToExpiry));
  }

  /**
   * Calculate d2 parameter for Black-Scholes.
   * d2 = d1 - σ√T
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The d2 parameter
   */
  public static double calculateD2(double underlyingSpotPrice, double strikePrice,
                                    double riskFreeRate, double timeToExpiry, double volatility) {
    return calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry, volatility)
        - volatility * Math.sqrt(timeToExpiry);
  }

  /**
   * Calculate call option price using Black-Scholes.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The theoretical call option price
   */
  public static double calculateCallOptionPrice(double underlyingSpotPrice, double strikePrice,
                                                 double riskFreeRate, double timeToExpiry,
                                                 double volatility) {
    if (timeToExpiry <= 0) {
      return Math.max(0.0, underlyingSpotPrice - strikePrice);
    }
    if (volatility <= 0) {
      return Math.max(0.0, underlyingSpotPrice - strikePrice);
    }

    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    double d2 = calculateD2(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);

    return underlyingSpotPrice * calculateNormalCumulativeDistribution(d1)
        - strikePrice * Math.exp(-riskFreeRate * timeToExpiry)
        * calculateNormalCumulativeDistribution(d2);
  }

  /**
   * Calculate put option price using Black-Scholes.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The theoretical put option price
   */
  public static double calculatePutOptionPrice(double underlyingSpotPrice, double strikePrice,
                                              double riskFreeRate, double timeToExpiry,
                                              double volatility) {
    if (timeToExpiry <= 0) {
      return Math.max(0.0, strikePrice - underlyingSpotPrice);
    }
    if (volatility <= 0) {
      return Math.max(0.0, strikePrice - underlyingSpotPrice);
    }

    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    double d2 = calculateD2(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);

    return strikePrice * Math.exp(-riskFreeRate * timeToExpiry)
        * calculateNormalCumulativeDistribution(-d2)
        - underlyingSpotPrice * calculateNormalCumulativeDistribution(-d1);
  }

  /**
   * Calculate call option delta.
   * Delta is the rate of change of the option price with respect to the underlying price.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The call option delta (between 0 and 1)
   */
  public static double calculateCallOptionDelta(double underlyingSpotPrice, double strikePrice,
                                                double riskFreeRate, double timeToExpiry,
                                                double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) {
      return underlyingSpotPrice > strikePrice ? 1.0 : 0.0;
    }
    return calculateNormalCumulativeDistribution(
        calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry, volatility));
  }

  /**
   * Calculate put option delta.
   * Delta is the rate of change of the option price with respect to the underlying price.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The put option delta (between -1 and 0)
   */
  public static double calculatePutOptionDelta(double underlyingSpotPrice, double strikePrice,
                                               double riskFreeRate, double timeToExpiry,
                                               double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) {
      return underlyingSpotPrice < strikePrice ? -1.0 : 0.0;
    }
    return calculateNormalCumulativeDistribution(
        calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry, volatility))
        - 1.0;
  }

  /**
   * Calculate option gamma (same for calls and puts).
   * Gamma is the rate of change of delta with respect to the underlying price.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The option gamma
   */
  public static double calculateGamma(double underlyingSpotPrice, double strikePrice,
                                       double riskFreeRate, double timeToExpiry,
                                       double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) return 0.0;
    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    return calculateNormalProbabilityDensity(d1)
        / (underlyingSpotPrice * volatility * Math.sqrt(timeToExpiry));
  }

  /**
   * Calculate option vega (same for calls and puts).
   * Vega is the rate of change of the option price with respect to volatility.
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The option vega (per 1% change in volatility)
   */
  public static double calculateVega(double underlyingSpotPrice, double strikePrice,
                                      double riskFreeRate, double timeToExpiry,
                                      double volatility) {
    if (timeToExpiry <= 0 || volatility <= 0) return 0.0;
    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    return underlyingSpotPrice * calculateNormalProbabilityDensity(d1)
        * Math.sqrt(timeToExpiry) / 100.0; // Divided by 100 for percentage IV
  }

  /**
   * Calculate call option theta.
   * Theta is the rate of change of the option price with respect to time (time decay).
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The call option theta (per day)
   */
  public static double calculateCallOptionTheta(double underlyingSpotPrice, double strikePrice,
                                                double riskFreeRate, double timeToExpiry,
                                                double volatility) {
    if (timeToExpiry <= 0) return 0.0;
    if (volatility <= 0) return 0.0;
    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    double d2 = calculateD2(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);

    double timeDecayTerm = -underlyingSpotPrice * calculateNormalProbabilityDensity(d1)
        * volatility / (2.0 * Math.sqrt(timeToExpiry));
    double interestRateTerm = riskFreeRate * strikePrice
        * Math.exp(-riskFreeRate * timeToExpiry)
        * calculateNormalCumulativeDistribution(d2);

    return (timeDecayTerm - interestRateTerm) / 365.0; // Per day
  }

  /**
   * Calculate put option theta.
   * Theta is the rate of change of the option price with respect to time (time decay).
   *
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param volatility Implied volatility (σ)
   * @return The put option theta (per day)
   */
  public static double calculatePutOptionTheta(double underlyingSpotPrice, double strikePrice,
                                             double riskFreeRate, double timeToExpiry,
                                             double volatility) {
    if (timeToExpiry <= 0) return 0.0;
    if (volatility <= 0) return 0.0;
    double d1 = calculateD1(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);
    double d2 = calculateD2(underlyingSpotPrice, strikePrice, riskFreeRate, timeToExpiry,
        volatility);

    double timeDecayTerm = -underlyingSpotPrice * calculateNormalProbabilityDensity(d1)
        * volatility / (2.0 * Math.sqrt(timeToExpiry));
    double interestRateTerm = riskFreeRate * strikePrice
        * Math.exp(-riskFreeRate * timeToExpiry)
        * calculateNormalCumulativeDistribution(-d2);

    return (timeDecayTerm + interestRateTerm) / 365.0; // Per day
  }

  /**
   * Calculate implied volatility using Newton-Raphson method.
   * Finds the volatility that makes the theoretical price equal to the market price.
   *
   * @param marketPrice Market price of the option
   * @param underlyingSpotPrice Current price of the underlying asset (S)
   * @param strikePrice Option strike price (K)
   * @param riskFreeRate Risk-free interest rate (r)
   * @param timeToExpiry Time to expiration in years (T)
   * @param isCall true for call option, false for put option
   * @return implied volatility or null if not found
   */
  public static Double calculateImpliedVolatility(double marketPrice, double underlyingSpotPrice,
                                                   double strikePrice, double riskFreeRate,
                                                   double timeToExpiry, boolean isCall) {
    if (timeToExpiry <= 0 || marketPrice <= 0) return null;

    double impliedVolatility = 0.5; // Initial guess: 50%
    double tolerance = 1e-6;
    int maxIterations = 100;

    for (int iteration = 0; iteration < maxIterations; iteration++) {
      double theoreticalPrice = isCall
          ? calculateCallOptionPrice(underlyingSpotPrice, strikePrice, riskFreeRate,
              timeToExpiry, impliedVolatility)
          : calculatePutOptionPrice(underlyingSpotPrice, strikePrice, riskFreeRate,
              timeToExpiry, impliedVolatility);

      double priceDifference = theoreticalPrice - marketPrice;
      if (Math.abs(priceDifference) < tolerance) {
        return impliedVolatility;
      }

      double vega = calculateVega(underlyingSpotPrice, strikePrice, riskFreeRate,
          timeToExpiry, impliedVolatility) * 100.0; // Scale back
      if (Math.abs(vega) < 1e-10) {
        return null; // Vega too small, can't converge
      }

      impliedVolatility = impliedVolatility - priceDifference / vega;

      // Bounds check
      if (impliedVolatility < 0.001) impliedVolatility = 0.001;
      if (impliedVolatility > 5.0) impliedVolatility = 5.0;
    }

    return impliedVolatility; // Return best guess if max iterations reached
  }
}
