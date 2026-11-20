package com.ilonggobistro.auth;

public enum OtpPurpose {
    PASSWORD_RESET("Password Reset"),
    PASSWORD_CHANGE("Password Change"),
    ACCOUNT_VERIFY("Contact Verification");

    private final String label;

    OtpPurpose(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
