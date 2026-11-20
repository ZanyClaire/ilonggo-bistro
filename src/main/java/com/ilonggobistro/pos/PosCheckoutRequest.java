package com.ilonggobistro.pos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PosCheckoutRequest(
        @Size(max = 120) String customerName,
        @Size(max = 30) String customerPhone,
        @NotNull OrderType orderType,
        @Size(max = 30) String tableNumber,
        @NotNull PaymentMethod paymentMethod,
        String discountType,
        BigDecimal customDiscountPhp,
        BigDecimal amountTenderedPhp,
        @Size(max = 500) String notes,
        @NotEmpty(message = "Add at least one dish to complete checkout")
        List<@Valid CartLine> items
) {
    public record CartLine(
            @NotNull Long menuItemId,
            @Min(1) int quantity
    ) {
    }
}
