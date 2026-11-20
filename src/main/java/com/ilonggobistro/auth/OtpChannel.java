package com.ilonggobistro.auth;

public enum OtpChannel {
    GMAIL("Gmail"),
    WHATSAPP("WhatsApp");

    private final String label;

    OtpChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
