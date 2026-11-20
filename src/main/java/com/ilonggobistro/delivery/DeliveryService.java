package com.ilonggobistro.delivery;

import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuItemRepository;
import com.ilonggobistro.pos.OrderType;
import com.ilonggobistro.pos.PaymentMethod;
import com.ilonggobistro.pos.PosCheckoutRequest;
import com.ilonggobistro.pos.PosOrder;
import com.ilonggobistro.pos.PosOrderItem;
import com.ilonggobistro.pos.PosOrderRepository;
import com.ilonggobistro.shared.ResourceNotFoundException;
import com.ilonggobistro.shared.StoreProfile;
import com.ilonggobistro.shared.StoreSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private static final String RECEIPT_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeliveryProperties properties;
    private final DeliveryRepository deliveryRepository;
    private final PosOrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final StoreSettingsService storeSettingsService;
    private final Map<DeliveryProvider, CourierApiClient> clients;
    private final Clock clock;

    public DeliveryService(DeliveryProperties properties,
                           DeliveryRepository deliveryRepository,
                           PosOrderRepository orderRepository,
                           MenuItemRepository menuItemRepository,
                           StoreSettingsService storeSettingsService,
                           List<CourierApiClient> clientList,
                           Clock clock) {
        this.properties = properties;
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.storeSettingsService = storeSettingsService;
        this.clients = clientList.stream().collect(Collectors.toMap(CourierApiClient::provider, Function.identity()));
        this.clock = clock;
    }

    public DeliveryDtos.DeliveryQuoteResponse getDeliveryQuote(double customerLat, double customerLng) {
        double rawKm = haversineKm(properties.getStoreLat(), properties.getStoreLng(), customerLat, customerLng);
        double distanceKm = BigDecimal.valueOf(rawKm).setScale(2, RoundingMode.HALF_UP).doubleValue();

        if (distanceKm > properties.getInhouseMaxKm()) {
            return new DeliveryDtos.DeliveryQuoteResponse(
                    distanceKm, BigDecimal.ZERO, "₱0.00", 0, "Out of range",
                    DeliveryProvider.IN_HOUSE.name(), DeliveryProvider.IN_HOUSE.getLabel(),
                    false, false,
                    "This pin is " + distanceKm + " km away (max delivery radius is " + properties.getInhouseMaxKm() + " km from Barotac Nuevo)."
            );
        }

        DeliveryProvider activeProvider = properties.resolvedProvider();
        if (activeProvider == DeliveryProvider.IN_HOUSE) {
            return buildInHouseQuote(distanceKm, "Dispatched by Ilonggo Bistro's local rider team.");
        }

        CourierApiClient client = clients.getOrDefault(activeProvider, clients.get(DeliveryProvider.SIMULATOR));
        try {
            CourierApiClient.QuoteEstimate estimate = client.requestQuote(
                    properties.getStoreLat(), properties.getStoreLng(),
                    customerLat, customerLng, distanceKm
            );
            String etaRange = Math.max(15, estimate.estimatedMinutes() - 5) + "–" + (estimate.estimatedMinutes() + 5) + " mins";
            String msg = activeProvider == DeliveryProvider.SIMULATOR
                    ? "Live quote calculated (Sandbox Simulator mode — add pandago/GrabExpress API keys anytime)."
                    : "Live courier quote via " + activeProvider.getLabel() + ".";

            return new DeliveryDtos.DeliveryQuoteResponse(
                    distanceKm, estimate.feePhp(), PosOrder.formatPhp(estimate.feePhp()),
                    estimate.estimatedMinutes(), etaRange,
                    activeProvider.name(), activeProvider.getLabel(),
                    true, false, msg
            );
        } catch (OutOfCoverageException ex) {
            return buildInHouseQuote(distanceKm,
                    "Outside third-party courier zone — covered by our In-House Local Rider!");
        }
    }

    @Transactional
    public PosOrder placeCustomerOrder(DeliveryDtos.OnlineOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Please add at least one dish to your order.");
        }
        boolean isDelivery = "DELIVERY".equalsIgnoreCase(request.fulfillmentType());
        if (isDelivery) {
            if (request.customerLat() == null || request.customerLng() == null) {
                throw new IllegalArgumentException("Please drop your pin on the map so we can calculate the delivery fee.");
            }
            if (request.dropoffAddress() == null || request.dropoffAddress().isBlank()) {
                throw new IllegalArgumentException("Please enter your complete street address.");
            }
        }

        List<PosOrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PosCheckoutRequest.CartLine line : request.items()) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be at least 1.");
            }
            MenuItem menuItem = menuItemRepository.findById(line.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item #" + line.menuItemId() + " not found."));
            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("\"" + menuItem.getName() + "\" is currently Sold Out.");
            }
            BigDecimal unit = menuItem.getPricePhp().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
            orderItems.add(new PosOrderItem(menuItem.getId(), menuItem.getName(), unit, line.quantity(), lineTotal));
        }

        DeliveryDtos.DeliveryQuoteResponse quote = null;
        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (isDelivery) {
            quote = getDeliveryQuote(request.customerLat(), request.customerLng());
            if (!quote.deliverable()) {
                throw new IllegalArgumentException(quote.message());
            }
            deliveryFee = quote.deliveryFeePhp().setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = subtotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);
        boolean isCod = "CASH_ON_DELIVERY".equalsIgnoreCase(request.paymentMode());
        PaymentMethod paymentMethod = isCod ? PaymentMethod.CASH : PaymentMethod.GCASH;
        String paymentStatus = isCod ? "CASH_ON_DELIVERY" : "PAID";

        LocalDateTime now = LocalDateTime.now(clock);
        String receiptNumber = generateUniqueReceiptNumber(now.getYear());

        PosOrder order = new PosOrder(
                receiptNumber,
                request.customerName().trim(),
                request.customerPhone().trim(),
                isDelivery ? OrderType.DELIVERY : OrderType.PICKUP,
                isDelivery ? "Delivery" : "Counter Pickup",
                paymentMethod,
                subtotal,
                null,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                grandTotal,
                isCod ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : grandTotal,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "online-web",
                request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null,
                now
        );
        order.setDeliveryFeePhp(deliveryFee);
        order.setPaymentStatus(paymentStatus);
        orderItems.forEach(order::addItem);
        PosOrder savedOrder = orderRepository.save(order);

        if (isDelivery && quote != null) {
            DeliveryProvider initialProvider = quote.inHouseFallback()
                    ? DeliveryProvider.IN_HOUSE
                    : DeliveryProvider.valueOf(quote.provider());
            Delivery delivery = new Delivery(
                    savedOrder,
                    initialProvider,
                    properties.getStoreAddress(),
                    properties.getStoreLat(),
                    properties.getStoreLng(),
                    request.dropoffAddress().trim(),
                    request.dropoffLandmark() != null ? request.dropoffLandmark().trim() : "",
                    request.customerLat(),
                    request.customerLng(),
                    BigDecimal.valueOf(quote.distanceKm()).setScale(2, RoundingMode.HALF_UP),
                    deliveryFee,
                    quote.estimatedMinutes()
            );
            if (quote.inHouseFallback()) {
                delivery.setFallbackReason("Outside third-party coverage — ready for In-House Local Rider assignment.");
            }
            deliveryRepository.save(delivery);

            if (properties.isAutoDispatch() && !quote.inHouseFallback()) {
                bookRider(savedOrder.getId());
            }
        }

        return savedOrder;
    }

    @Transactional
    public Delivery bookRider(Long orderId) {
        Delivery delivery = deliveryRepository.findWithOrderByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No delivery record found for order #" + orderId));
        PosOrder order = delivery.getOrder();
        StoreProfile profile = storeSettingsService.getProfile();

        DeliveryProvider targetProvider = properties.resolvedProvider();
        if (targetProvider == DeliveryProvider.IN_HOUSE) {
            delivery.setStatus(DeliveryStatus.OUT_OF_COVERAGE);
            delivery.setFallbackReason("Configured for In-House Local Rider mode — please enter rider details below.");
            return deliveryRepository.save(delivery);
        }

        CourierApiClient client = clients.getOrDefault(targetProvider, clients.get(DeliveryProvider.SIMULATOR));
        DeliveryDtos.CourierBookingRequest bookingReq = new DeliveryDtos.CourierBookingRequest(
                order.getReceiptNumber(),
                profile.bistroName(),
                profile.phoneNumber(),
                delivery.getPickupAddress(),
                delivery.getPickupLat(),
                delivery.getPickupLng(),
                order.getCustomerName(),
                order.getCustomerPhone() != null ? order.getCustomerPhone() : "",
                delivery.getDropoffAddress(),
                delivery.getDropoffLandmark(),
                delivery.getDropoffLat(),
                delivery.getDropoffLng(),
                order.getTotalPhp(),
                order.getPaymentStatus(),
                order.getItems().stream()
                        .map(i -> new DeliveryDtos.CourierBookingRequest.ItemSummary(
                                i.getItemName(), i.getQuantity(), i.getUnitPricePhp()))
                        .toList()
        );

        try {
            DeliveryDtos.CourierBookingResult result = client.bookDelivery(bookingReq);
            delivery.setProvider(result.provider());
            delivery.setExternalDeliveryId(result.externalDeliveryId());
            delivery.setStatus(result.initialStatus());
            delivery.setTrackingUrl(result.trackingUrl());
            if (result.riderName() != null) delivery.setRiderName(result.riderName());
            if (result.riderPhone() != null) delivery.setRiderPhone(result.riderPhone());
            if (result.riderPlateNumber() != null) delivery.setRiderPlateNumber(result.riderPlateNumber());
            delivery.setFallbackReason(null);
            delivery.setDispatchedAt(LocalDateTime.now(clock));
            log.info("Dispatched order {} via {} (externalId={})",
                    order.getReceiptNumber(), result.provider(), result.externalDeliveryId());
            return deliveryRepository.save(delivery);
        } catch (OutOfCoverageException ex) {
            log.warn("Courier out of coverage for {}: {}", order.getReceiptNumber(), ex.getMessage());
            delivery.setStatus(DeliveryStatus.OUT_OF_COVERAGE);
            delivery.setFallbackReason(ex.getMessage());
            return deliveryRepository.save(delivery);
        }
    }

    @Transactional
    public Delivery assignInHouseRider(Long orderId, String riderName, String riderPhone, String plateNumber) {
        if (riderName == null || riderName.isBlank() || riderPhone == null || riderPhone.isBlank()) {
            throw new IllegalArgumentException("Rider name and phone number are required.");
        }
        Delivery delivery = deliveryRepository.findWithOrderByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found for order #" + orderId));
        delivery.setProvider(DeliveryProvider.IN_HOUSE);
        delivery.setExternalDeliveryId("LOCAL-" + delivery.getOrder().getReceiptNumber());
        delivery.setRiderName(riderName.trim());
        delivery.setRiderPhone(riderPhone.trim());
        delivery.setRiderPlateNumber(plateNumber != null && !plateNumber.isBlank() ? plateNumber.trim().toUpperCase() : "BISTRO-MC");
        delivery.setTrackingUrl("/orders/" + delivery.getOrder().getReceiptNumber() + "#live-map");
        delivery.setStatus(DeliveryStatus.RIDER_ASSIGNED);
        delivery.setDispatchedAt(LocalDateTime.now(clock));
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery updateManualStatus(Long deliveryId, DeliveryStatus status) {
        Delivery delivery = deliveryRepository.findWithOrderById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found."));
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery handleWebhook(DeliveryDtos.DeliveryWebhookPayload payload, String providedSecret) {
        String configuredSecret = properties.getWebhookSecret();
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            if (providedSecret == null || !configuredSecret.equals(providedSecret)) {
                throw new SecurityException("Invalid delivery webhook secret.");
            }
        }

        Delivery delivery = null;
        if (payload.externalDeliveryId() != null && !payload.externalDeliveryId().isBlank()) {
            delivery = deliveryRepository.findWithOrderByExternalDeliveryId(payload.externalDeliveryId().trim()).orElse(null);
        }
        if (delivery == null && payload.orderReference() != null && !payload.orderReference().isBlank()) {
            delivery = deliveryRepository.findWithOrderByOrderReceiptNumberIgnoreCase(payload.orderReference().trim()).orElse(null);
        }
        if (delivery == null) {
            throw new ResourceNotFoundException("No matching delivery found for webhook payload.");
        }

        DeliveryStatus mappedStatus = mapWebhookStatus(payload.status());
        delivery.setStatus(mappedStatus);
        if (payload.riderName() != null && !payload.riderName().isBlank()) {
            delivery.setRiderName(payload.riderName().trim());
        }
        if (payload.riderPhone() != null && !payload.riderPhone().isBlank()) {
            delivery.setRiderPhone(payload.riderPhone().trim());
        }
        if (payload.plateNumber() != null && !payload.plateNumber().isBlank()) {
            delivery.setRiderPlateNumber(payload.plateNumber().trim().toUpperCase());
        }
        if (payload.trackingUrl() != null && !payload.trackingUrl().isBlank()) {
            delivery.setTrackingUrl(payload.trackingUrl().trim());
        }
        delivery.setLastWebhookPayload(payload.toString());
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Delivery simulateNextWebhook(Long deliveryId) {
        Delivery delivery = deliveryRepository.findWithOrderById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found."));

        DeliveryDtos.DeliveryWebhookPayload simulated = switch (delivery.getStatus()) {
            case ALLOCATING -> new DeliveryDtos.DeliveryWebhookPayload(
                    delivery.getExternalDeliveryId(),
                    delivery.getOrder().getReceiptNumber(),
                    "RIDER_ASSIGNED",
                    "Kuya Junrey Dela Torre",
                    "0917 842 3319",
                    "ILO-6042",
                    "/orders/" + delivery.getOrder().getReceiptNumber() + "#live-map"
            );
            case RIDER_ASSIGNED -> new DeliveryDtos.DeliveryWebhookPayload(
                    delivery.getExternalDeliveryId(),
                    delivery.getOrder().getReceiptNumber(),
                    "PICKED_UP",
                    delivery.getRiderName() != null ? delivery.getRiderName() : "Kuya Junrey Dela Torre",
                    delivery.getRiderPhone() != null ? delivery.getRiderPhone() : "0917 842 3319",
                    delivery.getRiderPlateNumber() != null ? delivery.getRiderPlateNumber() : "ILO-6042",
                    delivery.getTrackingUrl()
            );
            case PICKED_UP -> new DeliveryDtos.DeliveryWebhookPayload(
                    delivery.getExternalDeliveryId(),
                    delivery.getOrder().getReceiptNumber(),
                    "DELIVERED",
                    delivery.getRiderName(),
                    delivery.getRiderPhone(),
                    delivery.getRiderPlateNumber(),
                    delivery.getTrackingUrl()
            );
            default -> throw new IllegalArgumentException("Delivery is already in state " + delivery.getStatus().getLabel());
        };
        return handleWebhook(simulated, properties.getWebhookSecret());
    }

    @Transactional(readOnly = true)
    public List<Delivery> listDeliveries(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                return deliveryRepository.findByStatusOrderByCreatedAtDesc(
                        DeliveryStatus.valueOf(statusFilter.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return deliveryRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Delivery> findByOrderReceipt(String receiptNumber) {
        return deliveryRepository.findWithOrderByOrderReceiptNumberIgnoreCase(receiptNumber);
    }

    @Transactional(readOnly = true)
    public long countActiveDeliveries() {
        return deliveryRepository.countByStatusIn(List.of(
                DeliveryStatus.PENDING_DISPATCH,
                DeliveryStatus.OUT_OF_COVERAGE,
                DeliveryStatus.ALLOCATING,
                DeliveryStatus.RIDER_ASSIGNED,
                DeliveryStatus.PICKED_UP
        ));
    }

    public DeliveryProperties getProperties() {
        return properties;
    }

    private DeliveryDtos.DeliveryQuoteResponse buildInHouseQuote(double distanceKm, String message) {
        double extraKm = Math.max(0.0, distanceKm - 2.0);
        BigDecimal fee = BigDecimal.valueOf(40.0 + (extraKm * 10.0))
                .setScale(0, RoundingMode.CEILING)
                .setScale(2, RoundingMode.HALF_UP);
        int eta = (int) Math.round(22 + (distanceKm * 4.0));
        String etaRange = Math.max(18, eta - 5) + "–" + (eta + 5) + " mins";
        return new DeliveryDtos.DeliveryQuoteResponse(
                distanceKm, fee, PosOrder.formatPhp(fee), eta, etaRange,
                DeliveryProvider.IN_HOUSE.name(), DeliveryProvider.IN_HOUSE.getLabel(),
                true, true, message
        );
    }

    private DeliveryStatus mapWebhookStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Webhook status is required.");
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ALLOCATING", "SEARCHING", "NEW", "REQUESTED" -> DeliveryStatus.ALLOCATING;
            case "RIDER_ASSIGNED", "ACCEPTED", "COURIER_ASSIGNED", "PICKING_UP" -> DeliveryStatus.RIDER_ASSIGNED;
            case "PICKED_UP", "IN_DELIVERY", "ON_THE_WAY", "DISPATCHED" -> DeliveryStatus.PICKED_UP;
            case "DELIVERED", "COMPLETED" -> DeliveryStatus.DELIVERED;
            case "CANCELLED", "CANCELED", "FAILED" -> DeliveryStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported delivery webhook status: " + raw);
        };
    }

    private String generateUniqueReceiptNumber(int year) {
        for (int attempt = 0; attempt < 25; attempt++) {
            StringBuilder suffix = new StringBuilder(5);
            for (int i = 0; i < 5; i++) {
                suffix.append(RECEIPT_CHARS.charAt(RANDOM.nextInt(RECEIPT_CHARS.length())));
            }
            String candidate = "OR-" + year + "-" + suffix;
            if (!orderRepository.existsByReceiptNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique receipt number.");
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
