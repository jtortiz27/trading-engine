package com.ib.client;

import java.util.List;

/**
 * Stub implementation of the IBKR EClientSocket class.
 */
public class EClientSocket {
    public EClientSocket(EWrapper eWrapper) {}
    public EClientSocket(EWrapper eWrapper, EJavaSignal signal) {}
    public void eConnect(String host, int port, int clientId) {}
    public void eDisconnect() {}
    public boolean isConnected() { return false; }
    public void reqMktData(int reqId, Contract contract, String genericTicks, boolean snapshot, boolean regulatorySnapshot, List<TagValue> mktDataOptions) {}
    public void cancelMktData(int reqId) {}
    public void reqSecDefOptParams(int reqId, String underlyingSymbol, String futFopExchange, String underlyingSecType, int underlyingConId) {}
    public void placeOrder(int id, Contract contract, Order order) {}
}
