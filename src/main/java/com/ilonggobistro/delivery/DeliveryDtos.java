package com.ilonggobistro.delivery;

import com.ilonggobistro.pos.PosCheckoutRequest.CartLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class DeliveryDtos {

    private DeliveryDtos() {
    }

    public record QuoteRequest(
            @NotNull Double customerLat,
            @NotNull Double customerLng
    ) {
    }

    public record DeliveryQuoteResponse(
            double distanceKm,
            BigDecimal deliveryFeePhp,
            String formattedFee,
            int estimatedMinutes,
            String etaRange,
            String provider,
            String providerLabel,
            boolean deliverable,
            boolean inHouseFallback,
            String message
    ) {
    }

    public record CourierBookingRequest(
            String orderReference,
            String pickupName,
            String pickupPhone,
            String pickupAddress,
            double pickupLat,
            double pickupLng,
            String customerName,
            String customerPhone,
            String dropoffAddress,
            String dropoffLandmark,
            double dropoffLat,
            double dropoffLng,
            BigDecimal orderTotalPhp,
            String paymentMethod, // PAID or CASH_ON_DELIVERY
            List<ItemSummary> items
    ) {
        public record ItemSummary(String name, int quantity, BigDecimal unitPricePhp) {
        }
    }

    public record CourierBookingResult(
            DeliveryProvider provider,
            String externalDeliveryId,
            DeliveryStatus initialStatus,
            String riderName,
            String riderPhone,
            String riderPlateNumber,
            String trackingUrl,
            BigDecimal quotedFeePhp,
            int estimatedMinutes
    ) {
    }

    public record DeliveryWebhookPayload(
            String externalDeliveryId,
            String orderReference,
            @NotBlank String status,
            String riderName,
            String riderPhone,
            String plateNumber,
            String trackingUrl
    ) {
    }

    public record OnlineOrderRequest(
            @NotBlank(message = "Please enter your name") @Size(max = 120) String customerName,
            @NotBlank(message = "Phone number is required for delivery updates") @Size(max = 30) String customerPhone,
            @NotBlank String fulfillmentType, // DELIVERY or PICKUP
            @NotBlank String paymentMode,     // CASH_ON_DELIVERY or PAID
            Double customerLat,
            Double customerLng,
            @Size(max = 300) String dropoffAddress,
            @Size(max = 255) String dropoffLandmark,
            @Size(max = 500) String notes,
            @NotEmpty(message = "Add at least one dish to your order")
            List<@Valid CartLine> items
    ) {
    }
}
