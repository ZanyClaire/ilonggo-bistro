package com.ilonggobistro.pos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class PosOfflineSyncDtos {

    private PosOfflineSyncDtos() {
    }

    public record OfflineSyncBatchRequest(
            @NotEmpty(message = "At least one queued order is required to sync")
            List<@Valid OfflineOrderPayload> orders
    ) {
    }

    public record OfflineOrderPayload(
            @NotBlank @Size(max = 64) String clientOrderUuid,
            @NotBlank @Size(max = 36) String offlineReceiptNumber,
            String createdAtIso,
            @Size(max = 120) String customerName,
            @Size(max = 30) String customerPhone,
            @NotNull OrderType orderType,
            @Size(max = 30) String tableNumber,
            @NotNull PaymentMethod paymentMethod,
            String discountType,
            BigDecimal customDiscountPhp,
            BigDecimal amountTenderedPhp,
            @Size(max = 500) String notes,
            @NotEmpty List<@Valid OfflineItemLine> items
    ) {
    }

    public record OfflineItemLine(
            Long menuItemId,
            @NotBlank @Size(max = 140) String itemName,
            @NotNull BigDecimal unitPricePhp,
            @Min(1) int quantity
    ) {
    }

    public record OfflineOrderSyncOutcome(
            String clientOrderUuid,
            String offlineReceiptNumber,
            String serverReceiptNumber,
            String status, // SYNCED | ALREADY_SYNCED | FAILED
            String message
    ) {
    }

    public record OfflineSyncBatchResponse(
            int syncedCount,
            int alreadySyncedCount,
            int failedCount,
            String todayRevenueFormatted,
            long todayOrdersCount,
            List<OfflineOrderSyncOutcome> results
    ) {
    }
}
