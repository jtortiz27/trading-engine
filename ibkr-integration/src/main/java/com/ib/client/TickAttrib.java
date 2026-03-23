package com.ib.client;

/**
 * Tick attributes for price ticks.
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

    @Override
    public String toString() {
        return "TickAttrib{" +
                "canAutoExecute=" + canAutoExecute +
                ", pastLimit=" + pastLimit +
                ", preOpen=" + preOpen +
                '}';
    }
}