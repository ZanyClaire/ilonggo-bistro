package com.ilonggobistro.admin;

public enum StaffRole {
    ADMIN("Owner / Manager", "Full access to POS, Deliveries, Menu, Reservations, Events, Gallery, Staff & Settings"),
    CASHIER("Cashier", "Dashboard, POS Terminal & Receipts, Delivery Dispatch, and read-only Sales History"),
    BOOKING_HOST("Booking Host", "Dashboard, Table Reservations Queue, and Guest Inquiries"),
    KITCHEN_STAFF("Kitchen Staff", "Dashboard, Menu Manager, Categories, and view-only Delivery Board");

    private final String label;
    private final String permissionsSummary;

    StaffRole(String label, String permissionsSummary) {
        this.label = label;
        this.permissionsSummary = permissionsSummary;
    }

    public String getLabel() {
        return label;
    }

    public String getPermissionsSummary() {
        return permissionsSummary;
    }

    public static StaffRole fromStringOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return CASHIER;
        }
        try {
            return StaffRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CASHIER;
        }
    }
}
