package com.ilonggobistro.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registers Google OAuth2 Client ONLY when GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are non-blank.
 * Prevents startup failures when running locally without Google Cloud credentials.
 */
@Configuration
public class GoogleOAuth2Config {

    @Bean
    @ConditionalOnExpression("!'${app.google-oauth.client-id:}'.isBlank() and !'${app.google-oauth.client-secret:}'.isBlank()")
    public ClientRegistrationRepository clientRegistrationRepository(AuthNotificationProperties properties) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(properties.getGoogleOauth().getClientId())
                .clientSecret(properties.getGoogleOauth().getClientSecret())
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
