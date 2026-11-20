package com.ilonggobistro.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Production-ready pandago API client.
 * Activated automatically when DELIVERY_PROVIDER=PANDAGO and API_CLIENT_ID / API_CLIENT_SECRET are set.
 */
@Component
public class PandagoCourierClient implements CourierApiClient {

    private static final Logger log = LoggerFactory.getLogger(PandagoCourierClient.class);
    private static final String DEFAULT_SANDBOX_BASE = "https://pandago-api-sandbox.deliveryhero.io/ph/api/v1";

    private final DeliveryProperties properties;
    private final RestClient restClient;

    public PandagoCourierClient(DeliveryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public DeliveryProvider provider() {
        return DeliveryProvider.PANDAGO;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuoteEstimate requestQuote(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, double distanceKm) {
        String token = fetchAccessToken();
        String baseUrl = resolveBaseUrl();

        Map<String, Object> body = Map.of(
                "sender", Map.of("location", Map.of("latitude", pickupLat, "longitude", pickupLng)),
                "recipient", Map.of("location", Map.of("latitude", dropoffLat, "longitude", dropoffLng))
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/orders/fee")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            BigDecimal fee = response != null && response.get("estimated_delivery_fee") != null
                    ? new BigDecimal(response.get("estimated_delivery_fee").toString()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(59.00).setScale(2, RoundingMode.HALF_UP);
            int eta = (int) Math.round(20 + distanceKm * 3.5);
            return new QuoteEstimate(fee, eta);
        } catch (HttpClientErrorException.UnprocessableEntity | HttpClientErrorException.BadRequest ex) {
            throw new OutOfCoverageException("pandago reported drop-off outside delivery coverage: " + ex.getStatusText());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeliveryDtos.CourierBookingResult bookDelivery(DeliveryDtos.CourierBookingRequest request) {
        String token = fetchAccessToken();
        String baseUrl = resolveBaseUrl();

        Map<String, Object> sender = Map.of(
                "name", request.pickupName(),
                "phone_number", request.pickupPhone(),
                "location", Map.of(
                        "address", request.pickupAddress(),
                        "latitude", request.pickupLat(),
                        "longitude", request.pickupLng()
                )
        );
        Map<String, Object> recipient = Map.of(
                "name", request.customerName(),
                "phone_number", request.customerPhone(),
                "location", Map.of(
                        "address", request.dropoffAddress(),
                        "latitude", request.dropoffLat(),
                        "longitude", request.dropoffLng()
                ),
                "notes", request.dropoffLandmark() != null ? request.dropoffLandmark() : ""
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("client_order_id", request.orderReference());
        payload.put("sender", sender);
        payload.put("recipient", recipient);
        payload.put("payment_method", "CASH_ON_DELIVERY".equals(request.paymentMethod()) ? "CASH_ON_DELIVERY" : "PAID");
        payload.put("amount", request.orderTotalPhp());
        payload.put("description", "Ilonggo Bistro Order " + request.orderReference());

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/orders")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String extId = response != null && response.get("order_id") != null
                    ? response.get("order_id").toString()
                    : "PG-" + request.orderReference();
            String trackingUrl = response != null && response.get("tracking_link") != null
                    ? response.get("tracking_link").toString()
                    : "/orders/" + request.orderReference();
            BigDecimal fee = response != null && response.get("delivery_fee") != null
                    ? new BigDecimal(response.get("delivery_fee").toString()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(59.00);

            return new DeliveryDtos.CourierBookingResult(
                    DeliveryProvider.PANDAGO, extId, DeliveryStatus.ALLOCATING,
                    null, null, null, trackingUrl, fee, 30
            );
        } catch (HttpClientErrorException.UnprocessableEntity | HttpClientErrorException.BadRequest ex) {
            log.warn("pandago rejected booking for {} as out of coverage: {}", request.orderReference(), ex.getMessage());
            throw new OutOfCoverageException("pandago cannot service this drop-off location. Use In-House Local Rider.");
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchAccessToken() {
        String tokenUrl = resolveBaseUrl().replace("/ph/api/v1", "") + "/oauth2/token";
        Map<String, Object> tokenResp = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "grant_type", "client_credentials",
                        "client_id", properties.getApiClientId(),
                        "client_secret", properties.getApiClientSecret()
                ))
                .retrieve()
                .body(Map.class);
        if (tokenResp == null || tokenResp.get("access_token") == null) {
            throw new IllegalStateException("pandago OAuth token response did not contain access_token.");
        }
        return tokenResp.get("access_token").toString();
    }

    private String resolveBaseUrl() {
        return (properties.getApiBaseUrl() != null && !properties.getApiBaseUrl().isBlank())
                ? properties.getApiBaseUrl().replaceAll("/$", "")
                : DEFAULT_SANDBOX_BASE;
    }
}
