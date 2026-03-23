package com.ib.client;

/**
 * Describes a contract for symbol sampling.
 */
public class ContractDescription {
    private Contract contract;
    private String[] derivativeSecTypes;

    public ContractDescription() {
        contract = new Contract();
    }

    public Contract contract() {
        return contract;
    }

    public void contract(Contract contract) {
        this.contract = contract;
    }

    public String[] derivativeSecTypes() {
        return derivativeSecTypes;
    }

    public void derivativeSecTypes(String[] derivativeSecTypes) {
        this.derivativeSecTypes = derivativeSecTypes;
    }

    @Override
    public String toString() {
        return "ContractDescription{" +
                "contract=" + contract +
                '}';
    }
}