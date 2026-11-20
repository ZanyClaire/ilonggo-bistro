package com.ilonggobistro.pos;

public enum PrinterMode {
    BROWSER("Browser Thermal Print (Any USB / Bluetooth / OS Printer)"),
    WINDOWS_USB("Direct ESC/POS — Installed OS / USB / Bluetooth Printer"),
    NETWORK_TCP("Direct ESC/POS — Network LAN / Wi-Fi Printer (TCP Port 9100)");

    private final String label;

    PrinterMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PrinterMode fromStringOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return BROWSER;
        }
        try {
            return PrinterMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BROWSER;
        }
    }
}
