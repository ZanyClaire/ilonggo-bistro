package com.ilonggobistro.delivery;

import com.ilonggobistro.shared.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeliveryApiController {

    private final DeliveryService deliveryService;

    public DeliveryApiController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/delivery/quote")
    public ResponseEntity<DeliveryDtos.DeliveryQuoteResponse> quote(@Valid @RequestBody DeliveryDtos.QuoteRequest request) {
        DeliveryDtos.DeliveryQuoteResponse quote = deliveryService.getDeliveryQuote(
                request.customerLat(), request.customerLng()
        );
        return ResponseEntity.ok(quote);
    }

    @PostMapping("/webhooks/delivery")
    public ResponseEntity<?> receiveWebhook(
            @Valid @RequestBody DeliveryDtos.DeliveryWebhookPayload payload,
            @RequestHeader(value = "X-Delivery-Webhook-Secret", required = false) String webhookSecret) {
        try {
            Delivery updated = deliveryService.handleWebhook(payload, webhookSecret);
            return ResponseEntity.ok(Map.of(
                    "received", true,
                    "orderReference", updated.getOrder().getReceiptNumber(),
                    "status", updated.getStatus().name()
            ));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
