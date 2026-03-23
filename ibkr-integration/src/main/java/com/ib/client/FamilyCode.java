package com.ib.client;

/**
 * Represents a family code for account grouping.
 */
public class FamilyCode {
    private String accountId;
    private String familyCode;

    public FamilyCode() {
    }

    public String accountId() {
        return accountId;
    }

    public void accountId(String accountId) {
        this.accountId = accountId;
    }

    public String familyCode() {
        return familyCode;
    }

    public void familyCode(String familyCode) {
        this.familyCode = familyCode;
    }

    @Override
    public String toString() {
        return "FamilyCode{" +
                "accountId='" + accountId + '\'' +
                ", familyCode='" + familyCode + '\'' +
                '}';
    }
}