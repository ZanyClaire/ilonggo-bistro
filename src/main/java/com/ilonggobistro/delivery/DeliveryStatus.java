package com.ilonggobistro.delivery;

public enum DeliveryStatus {
    PENDING_DISPATCH("Awaiting Dispatch", 1),
    ALLOCATING("Finding Nearby Rider", 2),
    RIDER_ASSIGNED("Rider Assigned", 3),
    PICKED_UP("Out for Delivery", 4),
    DELIVERED("Delivered", 5),
    OUT_OF_COVERAGE("Out of Third-Party Coverage", 1),
    CANCELLED("Cancelled", 0);

    private final String label;
    private final int step;

    DeliveryStatus(String label, int step) {
        this.label = label;
        this.step = step;
    }

    public String getLabel() {
        return label;
    }

    public int getStep() {
        return step;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
