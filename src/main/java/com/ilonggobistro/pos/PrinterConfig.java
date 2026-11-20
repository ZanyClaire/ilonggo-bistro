package com.ilonggobistro.pos;

public record PrinterConfig(
        PrinterMode mode,
        String printerName,
        String networkHost,
        int networkPort,
        int paperWidthMm,
        boolean autoCut,
        boolean openCashDrawer,
        boolean autoPrintAfterCheckout,
        String footerNote
) {
    public int columns() {
        return paperWidthMm <= 58 ? 32 : 48;
    }

    public boolean isDirectEscPosMode() {
        return mode == PrinterMode.WINDOWS_USB || mode == PrinterMode.NETWORK_TCP;
    }

    public String targetSummary() {
        return switch (mode) {
            case BROWSER -> "Browser Print (" + paperWidthMm + "mm)";
            case WINDOWS_USB -> (printerName == null || printerName.isBlank())
                    ? "USB Printer (Not selected yet)"
                    : printerName + " (" + paperWidthMm + "mm ESC/POS)";
            case NETWORK_TCP -> (networkHost == null || networkHost.isBlank())
                    ? "LAN Printer (No IP set)"
                    : networkHost + ":" + networkPort + " (" + paperWidthMm + "mm ESC/POS)";
        };
    }
}
