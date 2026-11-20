package com.ilonggobistro.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AuthNotificationProperties {

    private Mail mail = new Mail();
    private Whatsapp whatsapp = new Whatsapp();
    private GoogleOauth googleOauth = new GoogleOauth();

    public boolean isMailSmtpConfigured() {
        return "SMTP".equalsIgnoreCase(mail.provider)
                && isNonBlank(mail.host)
                && isNonBlank(mail.username)
                && isNonBlank(mail.password);
    }

    public boolean isWhatsAppMetaConfigured() {
        return "META_CLOUD".equalsIgnoreCase(whatsapp.provider)
                && isNonBlank(whatsapp.apiToken)
                && isNonBlank(whatsapp.phoneNumberId);
    }

    public boolean isGoogleOAuthConfigured() {
        return isNonBlank(googleOauth.clientId) && isNonBlank(googleOauth.clientSecret);
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Whatsapp getWhatsapp() { return whatsapp; }
    public void setWhatsapp(Whatsapp whatsapp) { this.whatsapp = whatsapp; }
    public GoogleOauth getGoogleOauth() { return googleOauth; }
    public void setGoogleOauth(GoogleOauth googleOauth) { this.googleOauth = googleOauth; }

    public static class Mail {
        private String provider = "SIMULATOR";
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "no-reply@ilonggobistro.ph";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class Whatsapp {
        private String provider = "SIMULATOR";
        private String apiToken = "";
        private String phoneNumberId = "";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiToken() { return apiToken; }
        public void setApiToken(String apiToken) { this.apiToken = apiToken; }
        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    }

    public static class GoogleOauth {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }
}
