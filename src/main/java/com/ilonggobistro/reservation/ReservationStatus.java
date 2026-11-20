package com.ilonggobistro.reservation;

public enum ReservationStatus {
    PENDING("Pending Review"),
    CONFIRMED("Confirmed"),
    SEATED("Seated"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
