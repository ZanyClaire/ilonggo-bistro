package com.ilonggobistro.delivery;

import java.math.BigDecimal;

public interface CourierApiClient {

    DeliveryProvider provider();

    QuoteEstimate requestQuote(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng, double distanceKm);

    DeliveryDtos.CourierBookingResult bookDelivery(DeliveryDtos.CourierBookingRequest request);

    record QuoteEstimate(BigDecimal feePhp, int estimatedMinutes) {
    }
}
