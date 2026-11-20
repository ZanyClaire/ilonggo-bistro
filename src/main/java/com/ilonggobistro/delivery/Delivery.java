package com.ilonggobistro.delivery;

import com.ilonggobistro.pos.PosOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "deliveries")
public class Delivery {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private PosOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryProvider provider = DeliveryProvider.SIMULATOR;

    @Column(name = "external_delivery_id", length = 120)
    private String externalDeliveryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.PENDING_DISPATCH;

    @Column(name = "pickup_address", nullable = false, length = 255)
    private String pickupAddress;

    @Column(name = "pickup_lat", nullable = false)
    private double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private double pickupLng;

    @Column(name = "dropoff_address", nullable = false, length = 300)
    private String dropoffAddress;

    @Column(name = "dropoff_landmark", length = 255)
    private String dropoffLandmark;

    @Column(name = "dropoff_lat", nullable = false)
    private double dropoffLat;

    @Column(name = "dropoff_lng", nullable = false)
    private double dropoffLng;

    @Column(name = "distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "delivery_fee_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFeePhp;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes = 30;

    @Column(name = "rider_name", length = 120)
    private String riderName;

    @Column(name = "rider_phone", length = 40)
    private String riderPhone;

    @Column(name = "rider_plate_number", length = 40)
    private String riderPlateNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "fallback_reason", length = 255)
    private String fallbackReason;

    @Column(name = "last_webhook_payload", columnDefinition = "TEXT")
    private String lastWebhookPayload;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Delivery() {
    }

    public Delivery(PosOrder order, DeliveryProvider provider,
                    String pickupAddress, double pickupLat, double pickupLng,
                    String dropoffAddress, String dropoffLandmark, double dropoffLat, double dropoffLng,
                    BigDecimal distanceKm, BigDecimal deliveryFeePhp, int estimatedMinutes) {
        this.order = order;
        this.provider = provider;
        this.pickupAddress = pickupAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffAddress = dropoffAddress;
        this.dropoffLandmark = dropoffLandmark;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.distanceKm = distanceKm;
        this.deliveryFeePhp = deliveryFeePhp;
        this.estimatedMinutes = estimatedMinutes;
        this.status = DeliveryStatus.PENDING_DISPATCH;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String formattedFee() {
        return PosOrder.formatPhp(deliveryFeePhp);
    }

    public String formattedUpdatedAt() {
        return updatedAt != null ? updatedAt.format(FMT) : "";
    }

    public String etaRange() {
        int min = Math.max(15, estimatedMinutes - 5);
        int max = estimatedMinutes + 5;
        return min + "–" + max + " mins";
    }

    public boolean canDispatchCourier() {
        return status == DeliveryStatus.PENDING_DISPATCH || status == DeliveryStatus.OUT_OF_COVERAGE;
    }

    public boolean hasRiderAssigned() {
        return riderName != null && !riderName.isBlank();
    }

    public Long getId() { return id; }
    public PosOrder getOrder() { return order; }
    public DeliveryProvider getProvider() { return provider; }
    public void setProvider(DeliveryProvider provider) { this.provider = provider; }
    public String getExternalDeliveryId() { return externalDeliveryId; }
    public void setExternalDeliveryId(String externalDeliveryId) { this.externalDeliveryId = externalDeliveryId; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public String getPickupAddress() { return pickupAddress; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public String getDropoffAddress() { return dropoffAddress; }
    public String getDropoffLandmark() { return dropoffLandmark; }
    public double getDropoffLat() { return dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public BigDecimal getDeliveryFeePhp() { return deliveryFeePhp; }
    public void setDeliveryFeePhp(BigDecimal deliveryFeePhp) { this.deliveryFeePhp = deliveryFeePhp; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }
    public String getRiderPhone() { return riderPhone; }
    public void setRiderPhone(String riderPhone) { this.riderPhone = riderPhone; }
    public String getRiderPlateNumber() { return riderPlateNumber; }
    public void setRiderPlateNumber(String riderPlateNumber) { this.riderPlateNumber = riderPlateNumber; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public String getLastWebhookPayload() { return lastWebhookPayload; }
    public void setLastWebhookPayload(String lastWebhookPayload) { this.lastWebhookPayload = lastWebhookPayload; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
