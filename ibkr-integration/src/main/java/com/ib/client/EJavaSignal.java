package com.ib.client;

/**
 * Stub implementation of EJavaSignal from IBKR API.
 * This is used for thread synchronization in the message reader.
 */
public class EJavaSignal implements EReaderSignal {
    private boolean open = false;

    @Override
    public void issueSignal() {
        synchronized(this) {
            open = true;
            notifyAll();
        }
    }

    @Override
    public void waitForSignal() {
        synchronized(this) {
            while (!open) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            open = false;
        }
    }
}
