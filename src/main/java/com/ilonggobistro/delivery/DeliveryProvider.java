package com.ilonggobistro.delivery;

public enum DeliveryProvider {
    SIMULATOR("Pandago / GrabExpress (Sandbox Sim)"),
    PANDAGO("pandago"),
    GRABEXPRESS("GrabExpress"),
    IN_HOUSE("In-House Local Rider");

    private final String label;

    DeliveryProvider(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
