package com.ilonggobistro.pos;

public enum PosOrderStatus {
    PAID("Paid"),
    VOIDED("Voided");

    private final String label;

    PosOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
