package com.ilonggobistro.auth;

public interface MailDeliveryAdapter {
    boolean isSimulated();
    String providerLabel();
    void sendOtpEmail(String toEmail, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes);
}
