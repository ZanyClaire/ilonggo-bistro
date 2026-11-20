package com.ilonggobistro.delivery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Built-in local sandbox simulator for pandago / GrabExpress when live API keys
 * are not yet configured. Mirrors real courier quote, coverage limits, and
 * webhook-driven rider dispatch without requiring external credentials.
 */
@Component
public class SimulatorCourierClient implements CourierApiClient {

    private final DeliveryProperties properties;

    public SimulatorCourierClient(DeliveryProperties properties) {
        this.properties = properties;
    }

    @Override
    public DeliveryProvider provider() {
        return DeliveryProvider.SIMULATOR;
    }

    @Override
    public QuoteEstimate requestQuote(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, double distanceKm) {
        if (distanceKm > properties.getCourierMaxKm()) {
            throw new OutOfCoverageException("Address is " + String.format("%.1f", distanceKm)
                    + " km away, outside third-party courier coverage (" + properties.getCourierMaxKm() + " km).");
        }
        // Base PHP 45 for first 2 km + PHP 12 per extra km
        double extraKm = Math.max(0.0, distanceKm - 2.0);
        BigDecimal fee = BigDecimal.valueOf(45.0 + (extraKm * 12.0))
                .setScale(0, RoundingMode.CEILING)
                .setScale(2, RoundingMode.HALF_UP);
        int etaMinutes = (int) Math.round(18 + (distanceKm * 3.5));
        return new QuoteEstimate(fee, etaMinutes);
    }

    @Override
    public DeliveryDtos.CourierBookingResult bookDelivery(DeliveryDtos.CourierBookingRequest request) {
        double dist = DeliveryService.haversineKm(
                request.pickupLat(), request.pickupLng(),
                request.dropoffLat(), request.dropoffLng()
        );
        QuoteEstimate estimate = requestQuote(
                request.pickupLat(), request.pickupLng(),
                request.dropoffLat(), request.dropoffLng(), dist
        );
        String extId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String trackingUrl = "/orders/" + request.orderReference() + "#live-map";

        return new DeliveryDtos.CourierBookingResult(
                DeliveryProvider.SIMULATOR,
                extId,
                DeliveryStatus.ALLOCATING,
                null,
                null,
                null,
                trackingUrl,
                estimate.feePhp(),
                estimate.estimatedMinutes()
        );
    }
}
