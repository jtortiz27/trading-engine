package com.ib.client;

/**
 * Interface for signal mechanism used by EReader.
 */
public interface EReaderSignal {
    void issueSignal();
    void waitForSignal();
}
