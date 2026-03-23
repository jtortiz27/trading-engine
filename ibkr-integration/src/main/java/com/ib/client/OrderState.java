package com.ib.client;

/**
 * Stub implementation of OrderState from IBKR API.
 */
public class OrderState {
    private String status;
    private String initMarginBefore;
    private String maintMarginBefore;
    private String equityWithLoanBefore;
    private String initMarginAfter;
    private String maintMarginAfter;
    private String equityWithLoanAfter;
    private double commission;
    private double minCommission;
    private double maxCommission;
    private String commissionCurrency;
    private String warningText;
    private String completedTime;
    private String completedStatus;

    public String status() { return status; }
    public void status(String status) { this.status = status; }
    public String initMarginBefore() { return initMarginBefore; }
    public void initMarginBefore(String initMarginBefore) { this.initMarginBefore = initMarginBefore; }
    public String maintMarginBefore() { return maintMarginBefore; }
    public void maintMarginBefore(String maintMarginBefore) { this.maintMarginBefore = maintMarginBefore; }
    public String equityWithLoanBefore() { return equityWithLoanBefore; }
    public void equityWithLoanBefore(String equityWithLoanBefore) { this.equityWithLoanBefore = equityWithLoanBefore; }
    public String initMarginAfter() { return initMarginAfter; }
    public void initMarginAfter(String initMarginAfter) { this.initMarginAfter = initMarginAfter; }
    public String maintMarginAfter() { return maintMarginAfter; }
    public void maintMarginAfter(String maintMarginAfter) { this.maintMarginAfter = maintMarginAfter; }
    public String equityWithLoanAfter() { return equityWithLoanAfter; }
    public void equityWithLoanAfter(String equityWithLoanAfter) { this.equityWithLoanAfter = equityWithLoanAfter; }
    public double commission() { return commission; }
    public void commission(double commission) { this.commission = commission; }
    public double minCommission() { return minCommission; }
    public void minCommission(double minCommission) { this.minCommission = minCommission; }
    public double maxCommission() { return maxCommission; }
    public void maxCommission(double maxCommission) { this.maxCommission = maxCommission; }
    public String commissionCurrency() { return commissionCurrency; }
    public void commissionCurrency(String commissionCurrency) { this.commissionCurrency = commissionCurrency; }
    public String warningText() { return warningText; }
    public void warningText(String warningText) { this.warningText = warningText; }
    public String completedTime() { return completedTime; }
    public void completedTime(String completedTime) { this.completedTime = completedTime; }
    public String completedStatus() { return completedStatus; }
    public void completedStatus(String completedStatus) { this.completedStatus = completedStatus; }
}
