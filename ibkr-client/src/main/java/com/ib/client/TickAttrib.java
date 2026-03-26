package com.ib.client;

/**
 * Stub class of IBKR TickAttrib for compilation.
 * This is a placeholder - actual implementation requires TWS API JAR.
 */
public class TickAttrib {
    private boolean canAutoExecute;
    private boolean pastLimit;
    private boolean preOpen;
    
    public TickAttrib() {
    }
    
    public boolean canAutoExecute() {
        return canAutoExecute;
    }
    
    public void canAutoExecute(boolean canAutoExecute) {
        this.canAutoExecute = canAutoExecute;
    }
    
    public boolean pastLimit() {
        return pastLimit;
    }
    
    public void pastLimit(boolean pastLimit) {
        this.pastLimit = pastLimit;
    }
    
    public boolean preOpen() {
        return preOpen;
    }
    
    public void preOpen(boolean preOpen) {
        this.preOpen = preOpen;
    }
}
