package com.ilonggobistro.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Production Meta WhatsApp Business Cloud API adapter.
 * Activated automatically when WHATSAPP_PROVIDER=META_CLOUD and WHATSAPP_API_TOKEN / WHATSAPP_PHONE_NUMBER_ID are set.
 */
@Component
public class MetaCloudWhatsAppAdapter implements WhatsAppDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(MetaCloudWhatsAppAdapter.class);
    private final AuthNotificationProperties properties;
    private final RestClient restClient;

    public MetaCloudWhatsAppAdapter(AuthNotificationProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean isSimulated() {
        return false;
    }

    @Override
    public String providerLabel() {
        return "Meta WhatsApp Cloud API";
    }

    @Override
    public void sendOtpWhatsApp(String e164Digits, String recipientName, String otpCode, OtpPurpose purpose, int expiryMinutes) {
        AuthNotificationProperties.Whatsapp wa = properties.getWhatsapp();
        String url = "https://graph.facebook.com/v21.0/" + wa.getPhoneNumberId() + "/messages";

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", e164Digits,
                "type", "text",
                "text", Map.of(
                        "preview_url", false,
                        "body", "Ilonggo Bistro Staff Portal: Your " + purpose.getLabel()
                                + " OTP code is *" + otpCode + "* (valid for " + expiryMinutes + " minutes)."
                )
        );

        restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + wa.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Dispatched {} OTP via Meta WhatsApp Cloud API to +{}", purpose, e164Digits);
    }
}
