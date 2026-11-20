package com.ilonggobistro.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatorWhatsAppAdapter implements WhatsAppDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatorWhatsAppAdapter.class);

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public String providerLabel() {
        return "WhatsApp Simulator (Local Dev)";
    }

    @Override
    public void sendOtpWhatsApp(String e164Digits, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes) {
        log.info("[WHATSAPP SIMULATOR] To: +{} ({}) | Purpose: {} | 6-Digit OTP: {} (expires in {} min)",
                e164Digits, recipientName, purpose.getLabel(), otpCode, expiryMinutes);
    }
}
