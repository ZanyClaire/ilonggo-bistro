package com.ilonggobistro.pos;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "pos_orders")
public class PosOrder {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 28)
    private String receiptNumber;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 24)
    private OrderType orderType = OrderType.DINE_IN;

    @Column(name = "table_number", length = 30)
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "subtotal_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalPhp;

    @Column(name = "discount_label", length = 60)
    private String discountLabel;

    @Column(name = "discount_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountPhp = BigDecimal.ZERO;

    @Column(name = "delivery_fee_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFeePhp = BigDecimal.ZERO;

    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus = "PAID";

    @Column(name = "total_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPhp;

    @Column(name = "amount_tendered_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountTenderedPhp;

    @Column(name = "change_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal changePhp = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PosOrderStatus status = PosOrderStatus.PAID;

    @Column(name = "cashier_username", nullable = false, length = 80)
    private String cashierUsername;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PosOrderItem> items = new ArrayList<>();

    protected PosOrder() {
    }

    public PosOrder(String receiptNumber, String customerName, String customerPhone,
                    OrderType orderType, String tableNumber, PaymentMethod paymentMethod,
                    BigDecimal subtotalPhp, String discountLabel, BigDecimal discountPhp,
                    BigDecimal totalPhp, BigDecimal amountTenderedPhp, BigDecimal changePhp,
                    String cashierUsername, String notes, LocalDateTime createdAt) {
        this.receiptNumber = receiptNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.paymentMethod = paymentMethod;
        this.subtotalPhp = subtotalPhp;
        this.discountLabel = discountLabel;
        this.discountPhp = discountPhp;
        this.totalPhp = totalPhp;
        this.amountTenderedPhp = amountTenderedPhp;
        this.changePhp = changePhp;
        this.status = PosOrderStatus.PAID;
        this.cashierUsername = cashierUsername;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = PosOrderStatus.PAID;
        }
    }

    public void addItem(PosOrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public int totalItemCount() {
        return items.stream().mapToInt(PosOrderItem::getQuantity).sum();
    }

    public String formattedCreatedAt() {
        return createdAt != null ? createdAt.format(DATE_TIME_FMT) : "";
    }

    public String formattedSubtotal() {
        return formatPhp(subtotalPhp);
    }

    public String formattedDiscount() {
        return formatPhp(discountPhp);
    }

    public String formattedDeliveryFee() {
        return formatPhp(deliveryFeePhp);
    }

    public BigDecimal getDeliveryFeePhp() {
        return deliveryFeePhp;
    }

    public void setDeliveryFeePhp(BigDecimal deliveryFeePhp) {
        this.deliveryFeePhp = deliveryFeePhp != null ? deliveryFeePhp : BigDecimal.ZERO;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = (paymentStatus == null || paymentStatus.isBlank()) ? "PAID" : paymentStatus;
    }

    public String formattedTotal() {
        return formatPhp(totalPhp);
    }

    public String formattedTendered() {
        return formatPhp(amountTenderedPhp);
    }

    public String formattedChange() {
        return formatPhp(changePhp);
    }

    public static String formatPhp(BigDecimal amount) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₱" + nf.format(value);
    }

    public Long getId() {
        return id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getSubtotalPhp() {
        return subtotalPhp;
    }

    public String getDiscountLabel() {
        return discountLabel;
    }

    public BigDecimal getDiscountPhp() {
        return discountPhp;
    }

    public BigDecimal getTotalPhp() {
        return totalPhp;
    }

    public BigDecimal getAmountTenderedPhp() {
        return amountTenderedPhp;
    }

    public BigDecimal getChangePhp() {
        return changePhp;
    }

    public PosOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PosOrderStatus status) {
        this.status = status;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PosOrderItem> getItems() {
        return items;
    }
}
