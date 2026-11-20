package com.ilonggobistro.pos;

public enum OrderType {
    DINE_IN("Dine-In"),
    TAKEOUT("Takeout"),
    DELIVERY("Delivery"),
    PICKUP("Pickup");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
