package com.trading.model.options.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Black-Scholes option pricing and Greek calculations.
 * 
 * Used for:
 * - Theoretical option pricing
 * - Greek calculations when market data is incomplete
 * - Implied volatility solving
 */
public class BlackScholesCalculator {
    
    /**
     * Calculates option price and Greeks using Black-Scholes.
     */
    public static OptionResult calculate(double spot, double strike, double timeToExpiry,
                                        double riskFreeRate, double volatility, boolean isCall) {
        OptionResult result = new OptionResult();
        
        double d1 = calculateD1(spot, strike, timeToExpiry, riskFreeRate, volatility);
        double d2 = d1 - volatility * Math.sqrt(timeToExpiry);
        
        double nd1 = normalCdf(d1);
        double nd2 = normalCdf(d2);
        double nPrimed1 = normalPdf(d1);
        
        double discountFactor = Math.exp(-riskFreeRate * timeToExpiry);
        
        if (isCall) {
            // Call price
            double price = spot * nd1 - strike * discountFactor * nd2;
            result.setPrice(price);
            
            // Call Greeks
            result.setDelta(nd1);
            result.setGamma(nPrimed1 / (spot * volatility * Math.sqrt(timeToExpiry)));
            result.setTheta(-(spot * nPrimed1 * volatility) / (2 * Math.sqrt(timeToExpiry))
                - riskFreeRate * strike * discountFactor * nd2);
            result.setVega(spot * nPrimed1 * Math.sqrt(timeToExpiry) / 100.0); // Per 1%
            result.setRho(strike * timeToExpiry * discountFactor * nd2 / 100.0); // Per 1%
        } else {
            // Put price
            double price = strike * discountFactor * (1 - nd2) - spot * (1 - nd1);
            result.setPrice(price);
            
            // Put Greeks
            result.setDelta(nd1 - 1);
            result.setGamma(nPrimed1 / (spot * volatility * Math.sqrt(timeToExpiry)));
            result.setTheta(-(spot * nPrimed1 * volatility) / (2 * Math.sqrt(timeToExpiry))
                + riskFreeRate * strike * discountFactor * (1 - nd2));
            result.setVega(spot * nPrimed1 * Math.sqrt(timeToExpiry) / 100.0);
            result.setRho(-strike * timeToExpiry * discountFactor * (1 - nd2) / 100.0);
        }
        
        result.setImpliedVolatility(volatility);
        
        return result;
    }
    
    /**
     * Solves for implied volatility given market price.
     * Uses Newton-Raphson iteration.
     */
    public static Double solveImpliedVolatility(double spot, double strike, double timeToExpiry,
                                               double riskFreeRate, double marketPrice, 
                                               boolean isCall) {
        // Initial guess
        double volatility = 0.20;
        double tolerance = 0.0001;
        int maxIterations = 100;
        
        for (int i = 0; i < maxIterations; i++) {
            OptionResult result = calculate(spot, strike, timeToExpiry, riskFreeRate, 
                                           volatility, isCall);
            double price = result.getPrice();
            double vega = result.getVega() * 100.0; // Convert back
            
            double diff = price - marketPrice;
            
            if (Math.abs(diff) < tolerance) {
                return volatility;
            }
            
            if (vega < 1e-10) {
                break; // Vega too small, can't converge
            }
            
            // Newton-Raphson update
            volatility = volatility - diff / vega;
            
            // Keep within bounds
            volatility = Math.max(0.001, Math.min(2.0, volatility));
        }
        
        return volatility;
    }
    
    /**
     * Calculates probability of ITM/OTM.
     */
    public static ProbabilityResult calculateProbability(double spot, double strike, 
                                                        double timeToExpiry, double volatility) {
        double d2 = calculateD2(spot, strike, timeToExpiry, 0, volatility);
        
        ProbabilityResult result = new ProbabilityResult();
        result.setCallItmProbability(normalCdf(d2));
        result.setCallOtmProbability(1 - result.getCallItmProbability());
        result.setPutItmProbability(normalCdf(-d2));
        result.setPutOtmProbability(1 - result.getPutItmProbability());
        
        return result;
    }
    
    /**
     * Calculates the D1 term in Black-Scholes.
     */
    private static double calculateD1(double spot, double strike, double time, 
                                     double rate, double vol) {
        if (vol <= 0 || time <= 0) return 0;
        return (Math.log(spot / strike) + (rate + vol * vol / 2) * time) / 
               (vol * Math.sqrt(time));
    }
    
    /**
     * Calculates the D2 term in Black-Scholes.
     */
    private static double calculateD2(double spot, double strike, double time, 
                                     double rate, double vol) {
        return calculateD1(spot, strike, time, rate, vol) - vol * Math.sqrt(time);
    }
    
    /**
     * Standard normal CDF.
     */
    private static double normalCdf(double x) {
        // Abramowitz and Stegun approximation
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;
        
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x) / Math.sqrt(2.0);
        
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return 0.5 * (1.0 + sign * y);
    }
    
    /**
     * Standard normal PDF.
     */
    private static double normalPdf(double x) {
        return Math.exp(-x * x / 2.0) / Math.sqrt(2.0 * Math.PI);
    }
    
    // Result classes
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionResult {
        private double price;
        private double delta;
        private double gamma;
        private double theta;
        private double vega;
        private double rho;
        private double impliedVolatility;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProbabilityResult {
        private double callItmProbability;
        private double callOtmProbability;
        private double putItmProbability;
        private double putOtmProbability;
    }
}
