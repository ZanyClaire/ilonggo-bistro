package com.ilonggobistro.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatorMailAdapter implements MailDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatorMailAdapter.class);

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public String providerLabel() {
        return "Gmail Simulator (Local Dev)";
    }

    @Override
    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes) {
        log.info("[GMAIL SIMULATOR] To: {} ({}) | Purpose: {} | 6-Digit OTP: {} (expires in {} min)",
                toEmail, recipientName, purpose.getLabel(), otpCode, expiryMinutes);
    }
}
