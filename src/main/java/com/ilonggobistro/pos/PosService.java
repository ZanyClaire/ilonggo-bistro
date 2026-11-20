package com.ilonggobistro.pos;

import com.ilonggobistro.menu.MenuItem;
import com.ilonggobistro.menu.MenuItemRepository;
import com.ilonggobistro.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PosService {

    private static final BigDecimal SENIOR_PWD_RATE = new BigDecimal("0.20");
    private static final String RECEIPT_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PosOrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final Clock clock;

    public PosService(PosOrderRepository orderRepository,
                      MenuItemRepository menuItemRepository,
                      Clock clock) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.clock = clock;
    }

    @Transactional
    public PosOrder checkout(PosCheckoutRequest request, String cashierUsername) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Please add at least one item to the order.");
        }

        List<PosOrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PosCheckoutRequest.CartLine line : request.items()) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be at least 1.");
            }
            MenuItem menuItem = menuItemRepository.findById(line.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item #" + line.menuItemId() + " no longer exists."));
            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("\"" + menuItem.getName() + "\" is currently marked Sold Out.");
            }

            BigDecimal unitPrice = menuItem.getPricePhp().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
            orderItems.add(new PosOrderItem(menuItem.getId(), menuItem.getName(), unitPrice, line.quantity(), lineTotal));
        }

        // Calculate discount safely on the server
        String discountType = request.discountType() != null ? request.discountType().trim().toUpperCase() : "NONE";
        String discountLabel = null;
        BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if ("SENIOR_PWD".equals(discountType)) {
            discountLabel = "Senior / PWD (20%)";
            discount = subtotal.multiply(SENIOR_PWD_RATE).setScale(2, RoundingMode.HALF_UP);
        } else if ("CUSTOM".equals(discountType) && request.customDiscountPhp() != null
                && request.customDiscountPhp().compareTo(BigDecimal.ZERO) > 0) {
            discountLabel = "Custom Discount";
            discount = request.customDiscountPhp().min(subtotal).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal tendered;
        if (request.paymentMethod() == PaymentMethod.CASH) {
            if (request.amountTenderedPhp() == null || request.amountTenderedPhp().compareTo(total) < 0) {
                throw new IllegalArgumentException("Cash tendered must be at least " + PosOrder.formatPhp(total) + ".");
            }
            tendered = request.amountTenderedPhp().setScale(2, RoundingMode.HALF_UP);
        } else {
            tendered = (request.amountTenderedPhp() != null && request.amountTenderedPhp().compareTo(total) >= 0)
                    ? request.amountTenderedPhp().setScale(2, RoundingMode.HALF_UP)
                    : total;
        }
        BigDecimal change = tendered.subtract(total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now(clock);
        String customerName = (request.customerName() == null || request.customerName().isBlank())
                ? "Walk-in Guest" : request.customerName().trim();
        String customerPhone = (request.customerPhone() == null || request.customerPhone().isBlank())
                ? null : request.customerPhone().trim();
        String tableNumber = request.orderType() == OrderType.DINE_IN
                ? ((request.tableNumber() == null || request.tableNumber().isBlank()) ? "Counter" : request.tableNumber().trim())
                : "Takeout";
        String notes = (request.notes() == null || request.notes().isBlank()) ? null : request.notes().trim();
        String cashier = (cashierUsername == null || cashierUsername.isBlank()) ? "staff" : cashierUsername;

        String receiptNumber = generateUniqueReceiptNumber(now.getYear());

        PosOrder order = new PosOrder(
                receiptNumber,
                customerName,
                customerPhone,
                request.orderType(),
                tableNumber,
                request.paymentMethod(),
                subtotal,
                discountLabel,
                discount,
                total,
                tendered,
                change,
                cashier,
                notes,
                now
        );
        orderItems.forEach(order::addItem);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public PosOrder getByReceiptNumber(String receiptNumber) {
        return orderRepository.findWithItemsByReceiptNumberIgnoreCase(receiptNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Receipt " + receiptNumber + " was not found."));
    }

    @Transactional(readOnly = true)
    public List<PosOrder> searchHistory(String query, String paymentFilter, String statusFilter,
                                        LocalDate fromDate, LocalDate toDate) {
        String cleanQuery = (query == null || query.isBlank()) ? null : query.trim();
        PaymentMethod paymentMethod = parsePaymentMethod(paymentFilter);
        PosOrderStatus status = parseStatus(statusFilter);
        LocalDateTime fromTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        return orderRepository.searchHistory(cleanQuery, paymentMethod, status, fromTime, toTime);
    }

    @Transactional(readOnly = true)
    public List<PosOrder> listRecent() {
        return orderRepository.findTop6ByOrderByCreatedAtDesc();
    }

    @Transactional
    public PosOrder voidOrder(Long orderId) {
        PosOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        order.setStatus(PosOrderStatus.VOIDED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public SalesSummary getSalesSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        BigDecimal todayRevenue = orderRepository.sumRevenueBetween(PosOrderStatus.PAID, startOfDay, endOfDay);
        long todayOrders = orderRepository.countOrdersBetween(PosOrderStatus.PAID, startOfDay, endOfDay);
        BigDecimal allTimeRevenue = orderRepository.sumAllRevenue(PosOrderStatus.PAID);
        long allTimeOrders = orderRepository.countByStatus(PosOrderStatus.PAID);

        BigDecimal averageTicket = allTimeOrders > 0
                ? allTimeRevenue.divide(BigDecimal.valueOf(allTimeOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new SalesSummary(todayRevenue, todayOrders, allTimeRevenue, allTimeOrders, averageTicket);
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private PosOrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return PosOrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    public record SalesSummary(
            BigDecimal todayRevenue,
            long todayOrders,
            BigDecimal allTimeRevenue,
            long allTimeOrders,
            BigDecimal averageTicket
    ) {
        public String formattedTodayRevenue() {
            return PosOrder.formatPhp(todayRevenue);
        }

        public String formattedAllTimeRevenue() {
            return PosOrder.formatPhp(allTimeRevenue);
        }

        public String formattedAverageTicket() {
            return PosOrder.formatPhp(averageTicket);
        }
    }
}
