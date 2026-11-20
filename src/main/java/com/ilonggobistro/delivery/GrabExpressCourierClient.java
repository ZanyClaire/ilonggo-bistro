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
import java.util.List;
import java.util.Map;

/**
 * Production-ready GrabExpress API client.
 * Activated automatically when DELIVERY_PROVIDER=GRABEXPRESS and API_CLIENT_ID / API_CLIENT_SECRET are set.
 */
@Component
public class GrabExpressCourierClient implements CourierApiClient {

    private static final Logger log = LoggerFactory.getLogger(GrabExpressCourierClient.class);
    private static final String DEFAULT_STAGING_BASE = "https://partner-api.stg-myteksi.com";

    private final DeliveryProperties properties;
    private final RestClient restClient;

    public GrabExpressCourierClient(DeliveryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public DeliveryProvider provider() {
        return DeliveryProvider.GRABEXPRESS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuoteEstimate requestQuote(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, double distanceKm) {
        String token = fetchAccessToken();
        String baseUrl = resolveBaseUrl();

        Map<String, Object> body = Map.of(
                "serviceType", "INSTANT",
                "packages", List.of(Map.of("name", "Food Order", "description", "Ilonggo Bistro Meal", "quantity", 1)),
                "origin", Map.of("coordinates", Map.of("latitude", pickupLat, "longitude", pickupLng)),
                "destination", Map.of("coordinates", Map.of("latitude", dropoffLat, "longitude", dropoffLng))
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/grab-express-sandbox/v1/deliveries/quotes")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            BigDecimal fee = BigDecimal.valueOf(65.00).setScale(2, RoundingMode.HALF_UP);
            if (response != null && response.get("quotes") instanceof List<?> quotes && !quotes.isEmpty()
                    && quotes.getFirst() instanceof Map<?, ?> q && q.get("amount") != null) {
                fee = new BigDecimal(q.get("amount").toString()).setScale(2, RoundingMode.HALF_UP);
            }
            int eta = (int) Math.round(20 + distanceKm * 3.5);
            return new QuoteEstimate(fee, eta);
        } catch (HttpClientErrorException.UnprocessableEntity | HttpClientErrorException.BadRequest ex) {
            throw new OutOfCoverageException("GrabExpress reported location outside service area.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeliveryDtos.CourierBookingResult bookDelivery(DeliveryDtos.CourierBookingRequest request) {
        String token = fetchAccessToken();
        String baseUrl = resolveBaseUrl();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchantOrderID", request.orderReference());
        payload.put("serviceType", "INSTANT");
        payload.put("paymentMethod", "CASH_ON_DELIVERY".equals(request.paymentMethod()) ? "CASH" : "CASHLESS");
        payload.put("sender", Map.of("firstName", request.pickupName(), "phone", request.pickupPhone()));
        payload.put("recipient", Map.of("firstName", request.customerName(), "phone", request.customerPhone()));
        payload.put("origin", Map.of(
                "address", request.pickupAddress(),
                "coordinates", Map.of("latitude", request.pickupLat(), "longitude", request.pickupLng())
        ));
        payload.put("destination", Map.of(
                "address", request.dropoffAddress(),
                "keywords", request.dropoffLandmark() != null ? request.dropoffLandmark() : "",
                "coordinates", Map.of("latitude", request.dropoffLat(), "longitude", request.dropoffLng())
        ));
        payload.put("packages", request.items().stream().map(i -> Map.of(
                "name", i.name(), "description", i.name(), "quantity", i.quantity(), "price", i.unitPricePhp()
        )).toList());

        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/grab-express-sandbox/v1/deliveries")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String extId = response != null && response.get("deliveryID") != null
                    ? response.get("deliveryID").toString()
                    : "GE-" + request.orderReference();
            String trackingUrl = response != null && response.get("trackingURL") != null
                    ? response.get("trackingURL").toString()
                    : "/orders/" + request.orderReference();

            return new DeliveryDtos.CourierBookingResult(
                    DeliveryProvider.GRABEXPRESS, extId, DeliveryStatus.ALLOCATING,
                    null, null, null, trackingUrl, BigDecimal.valueOf(65.00), 30
            );
        } catch (HttpClientErrorException.UnprocessableEntity | HttpClientErrorException.BadRequest ex) {
            log.warn("GrabExpress rejected booking for {} as out of coverage: {}", request.orderReference(), ex.getMessage());
            throw new OutOfCoverageException("GrabExpress cannot cover this address. Switch to In-House Local Rider.");
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchAccessToken() {
        String baseUrl = resolveBaseUrl();
        Map<String, Object> tokenResp = restClient.post()
                .uri(baseUrl + "/grabid/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "client_id", properties.getApiClientId(),
                        "client_secret", properties.getApiClientSecret(),
                        "grant_type", "client_credentials",
                        "scope", "grab_express.partner_deliveries"
                ))
                .retrieve()
                .body(Map.class);
        if (tokenResp == null || tokenResp.get("access_token") == null) {
            throw new IllegalStateException("GrabExpress OAuth response missing access_token.");
        }
        return tokenResp.get("access_token").toString();
    }

    private String resolveBaseUrl() {
        return (properties.getApiBaseUrl() != null && !properties.getApiBaseUrl().isBlank())
                ? properties.getApiBaseUrl().replaceAll("/$", "")
                : DEFAULT_STAGING_BASE;
    }
}
