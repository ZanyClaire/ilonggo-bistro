package com.ilonggobistro.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.delivery")
public class DeliveryProperties {

    private String provider = "SIMULATOR";
    private String apiClientId = "";
    private String apiClientSecret = "";
    private String apiBaseUrl = "";
    private String webhookSecret = "";
    private double storeLat = 10.8969;
    private double storeLng = 122.7022;
    private String storeAddress = "Burgos-Osmeña St, Barotac Nuevo, 5007 Iloilo";
    private boolean autoDispatch = false;
    private double courierMaxKm = 8.0;
    private double inhouseMaxKm = 15.0;

    public DeliveryProvider resolvedProvider() {
        if (provider == null || provider.isBlank()) {
            return DeliveryProvider.SIMULATOR;
        }
        try {
            DeliveryProvider parsed = DeliveryProvider.valueOf(provider.trim().toUpperCase());
            if ((parsed == DeliveryProvider.PANDAGO || parsed == DeliveryProvider.GRABEXPRESS)
                    && !hasLiveApiCredentials()) {
                return DeliveryProvider.SIMULATOR;
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            return DeliveryProvider.SIMULATOR;
        }
    }

    public boolean hasLiveApiCredentials() {
        return apiClientId != null && !apiClientId.isBlank()
                && apiClientSecret != null && !apiClientSecret.isBlank();
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiClientId() {
        return apiClientId;
    }

    public void setApiClientId(String apiClientId) {
        this.apiClientId = apiClientId;
    }

    public String getApiClientSecret() {
        return apiClientSecret;
    }

    public void setApiClientSecret(String apiClientSecret) {
        this.apiClientSecret = apiClientSecret;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public double getStoreLat() {
        return storeLat;
    }

    public void setStoreLat(double storeLat) {
        this.storeLat = storeLat;
    }

    public double getStoreLng() {
        return storeLng;
    }

    public void setStoreLng(double storeLng) {
        this.storeLng = storeLng;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public boolean isAutoDispatch() {
        return autoDispatch;
    }

    public void setAutoDispatch(boolean autoDispatch) {
        this.autoDispatch = autoDispatch;
    }

    public double getCourierMaxKm() {
        return courierMaxKm;
    }

    public void setCourierMaxKm(double courierMaxKm) {
        this.courierMaxKm = courierMaxKm;
    }

    public double getInhouseMaxKm() {
        return inhouseMaxKm;
    }

    public void setInhouseMaxKm(double inhouseMaxKm) {
        this.inhouseMaxKm = inhouseMaxKm;
    }
}
