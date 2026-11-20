package com.ilonggobistro.kitchen;

public enum KitchenStatus {
    PENDING("Up Next", "Waiting in Kitchen", "Waiting in Kitchen", 1),
    COOKING("In Progress", "Cooking Now", "Cooking Now", 2),
    READY("Ready", "Ready for Pickup / Rider", "Ready for Pickup / Rider", 3),
    SERVED("Served", "Served / Handed Off", "Completed / Handed Off", 4);

    private final String boardLabel;
    private final String staffLabel;
    private final String customerLabel;
    private final int step;

    KitchenStatus(String boardLabel, String staffLabel, String customerLabel, int step) {
        this.boardLabel = boardLabel;
        this.staffLabel = staffLabel;
        this.customerLabel = customerLabel;
        this.step = step;
    }

    public String getBoardLabel() {
        return boardLabel;
    }

    public String getStaffLabel() {
        return staffLabel;
    }

    public String getCustomerLabel() {
        return customerLabel;
    }

    public int getStep() {
        return step;
    }

    public boolean isActiveOnBoard() {
        return this == PENDING || this == COOKING || this == READY;
    }

    public String badgeToneClasses() {
        return switch (this) {
            case PENDING -> "bg-amber-50 text-amber-900 border-amber-200";
            case COOKING -> "bg-orange-100 text-orange-950 border-orange-300";
            case READY -> "bg-emerald-100 text-emerald-900 border-emerald-300";
            case SERVED -> "bg-linen text-slatewarm border-sand";
        };
    }

    public String customerStatusLabel(boolean isDelivery) {
        return switch (this) {
            case PENDING -> "Waiting in Kitchen";
            case COOKING -> "Cooking Now";
            case READY -> isDelivery ? "Ready for Rider Pickup" : "Ready for Counter Pickup";
            case SERVED -> isDelivery ? "Handed Off to Rider" : "Picked Up / Served";
        };
    }
}
