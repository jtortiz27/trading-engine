package com.ib.client;

/**
 * Stub implementation of ContractDetails from IBKR API.
 */
public class ContractDetails {
    private Contract contract;
    private String marketName;
    private double minTick;
    private String orderTypes;
    private String validExchanges;
    private long priceMagnifier;
    private int underConId;
    private String longName;
    private String contractMonth;
    private String industry;
    private String category;
    private String subcategory;
    private String timeZoneId;
    private String tradingHours;
    private String liquidHours;
    private String evRule;
    private double evMultiplier;
    private int mdSizeMultiplier;
    private int aggGroup;
    private String underSymbol;
    private String underSecType;
    private String marketRuleIds;
    private String realExpirationDate;
    private String lastTradeTime;
    private String stockType;
    private double minSize;
    private double sizeIncrement;
    private double suggestedSizeIncrement;
    private double fundValueMultiplier;
    private double fundNavPriceMultiplier;
    private String bondType;
    private String couponType;
    private boolean callable;
    private boolean putable;
    private String coupon;
    private boolean convertible;
    private String maturity;
    private String issueDate;
    private String nextOptionDate;
    private String nextOptionType;
    private boolean nextOptionPartial;
    private String notes;

    public Contract contract() { return contract; }
    public void contract(Contract contract) { this.contract = contract; }
    public String marketName() { return marketName; }
    public void marketName(String marketName) { this.marketName = marketName; }
    public double minTick() { return minTick; }
    public void minTick(double minTick) { this.minTick = minTick; }
    public String orderTypes() { return orderTypes; }
    public void orderTypes(String orderTypes) { this.orderTypes = orderTypes; }
    public String validExchanges() { return validExchanges; }
    public void validExchanges(String validExchanges) { this.validExchanges = validExchanges; }
    public long priceMagnifier() { return priceMagnifier; }
    public void priceMagnifier(long priceMagnifier) { this.priceMagnifier = priceMagnifier; }
    public int underConId() { return underConId; }
    public void underConId(int underConId) { this.underConId = underConId; }
    public String longName() { return longName; }
    public void longName(String longName) { this.longName = longName; }
    public String contractMonth() { return contractMonth; }
    public void contractMonth(String contractMonth) { this.contractMonth = contractMonth; }
}
