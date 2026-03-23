package com.trading.model.options.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full options chain data structure for analytics.
 * Organized by expiration slices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionsChainData {
    private String underlyingSymbol;
    private Double underlyingPrice;
    private LocalDate timestamp;
    private List<ExpirationSlice> expirations;
    
    // Chain-wide metrics
    private Double historicalVol10;    // 10-day realized vol
    private Double historicalVol20;    // 20-day realized vol
    private Double medianGap;          // Median overnight gap
    private Double percentile90Gap;    // 90th percentile overnight gap
    
    public OptionsChainData(String symbol, Double price) {
        this.underlyingSymbol = symbol;
        this.underlyingPrice = price;
        this.timestamp = LocalDate.now();
        this.expirations = new ArrayList<>();
    }
    
    /**
     * Gets the expiration slice for a specific date.
     */
    public ExpirationSlice getExpiration(LocalDate date) {
        if (expirations == null) return null;
        return expirations.stream()
            .filter(e -> e.getExpiryDate().equals(date))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets the expiration slice closest to target DTE.
     */
    public ExpirationSlice getExpirationByDte(int targetDte) {
        if (expirations == null || expirations.isEmpty()) return null;
        return expirations.stream()
            .min(Comparator.comparing(e -> 
                Math.abs(e.getDaysToExpiry() - targetDte)))
            .orElse(null);
    }
    
    /**
     * Gets expirations sorted by days to expiry.
     */
    public List<ExpirationSlice> getExpirationsByTenor() {
        if (expirations == null) return new ArrayList<>();
        return expirations.stream()
            .sorted(Comparator.comparing(ExpirationSlice::getDaysToExpiry))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all strikes across all expirations.
     */
    public List<OptionStrike> getAllStrikes() {
        if (expirations == null) return new ArrayList<>();
        return expirations.stream()
            .flatMap(e -> e.getStrikes() != null ? e.getStrikes().stream() : 
                new ArrayList<OptionStrike>().stream())
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the front month expiration (shortest DTE).
     */
    public ExpirationSlice getFrontMonth() {
        if (expirations == null || expirations.isEmpty()) return null;
        return expirations.stream()
            .min(Comparator.comparing(ExpirationSlice::getDaysToExpiry))
            .orElse(null);
    }
    
    /**
     * Gets the ~30 DTE expiration.
     */
    public ExpirationSlice get30dExpiration() {
        return getExpirationByDte(30);
    }
    
    /**
     * Gets the ~60 DTE expiration.
     */
    public ExpirationSlice get60dExpiration() {
        return getExpirationByDte(60);
    }
    
    /**
     * Calculates VRP (Volatility Risk Premium) for ATM options.
     */
    public Double calculateVrp() {
        ExpirationSlice front = getFrontMonth();
        if (front == null || front.getAtmIv() == null || historicalVol20 == null) {
            return null;
        }
        double iv = front.getAtmIv();
        return (iv * iv) - (historicalVol20 * historicalVol20);
    }
    
    /**
     * Returns summary statistics for the chain.
     */
    public ChainSummary getSummary() {
        ChainSummary summary = new ChainSummary();
        summary.setSymbol(underlyingSymbol);
        summary.setSpot(underlyingPrice);
        summary.setTimestamp(timestamp);
        
        ExpirationSlice front = getFrontMonth();
        if (front != null) {
            summary.setFrontDte(front.getDaysToExpiry());
            summary.setFrontAtmIv(front.getAtmIv());
            summary.setFrontPutCallRatio(front.getPutCallRatio());
        }
        
        summary.setNumExpirations(expirations != null ? expirations.size() : 0);
        summary.setHv20(historicalVol20);
        summary.setVrp(calculateVrp());
        
        return summary;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainSummary {
        private String symbol;
        private Double spot;
        private LocalDate timestamp;
        private Integer frontDte;
        private Double frontAtmIv;
        private Double frontPutCallRatio;
        private Integer numExpirations;
        private Double hv20;
        private Double vrp;
    }
}
