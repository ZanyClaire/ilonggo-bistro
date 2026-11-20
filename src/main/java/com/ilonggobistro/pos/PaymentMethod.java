package com.ilonggobistro.pos;

public enum PaymentMethod {
    CASH("Cash"),
    GCASH("GCash"),
    MAYA("Maya"),
    CARD("Card");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
