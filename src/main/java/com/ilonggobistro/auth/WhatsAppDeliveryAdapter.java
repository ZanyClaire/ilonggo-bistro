package com.ilonggobistro.auth;

public interface WhatsAppDeliveryAdapter {
    boolean isSimulated();
    String providerLabel();
    void sendOtpWhatsApp(String e164Digits, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes);
}
