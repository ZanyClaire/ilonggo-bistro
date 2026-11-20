package com.ilonggobistro.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePhp;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "badge_label", length = 40)
    private String badgeLabel;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MenuItem() {
    }

    public MenuItem(MenuCategory category, String name, String description, BigDecimal pricePhp,
                    String imageUrl, String badgeLabel, boolean featured, boolean available, int displayOrder) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.pricePhp = pricePhp;
        this.imageUrl = imageUrl;
        this.badgeLabel = badgeLabel;
        this.featured = featured;
        this.available = available;
        this.displayOrder = displayOrder;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public String formattedPrice() {
        if (pricePhp == null) {
            return "₱0.00";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return "₱" + nf.format(pricePhp);
    }

    public String effectiveImageUrl() {
        return (imageUrl == null || imageUrl.isBlank()) ? "/images/dish-batchoy.jpg" : imageUrl;
    }

    public Long getId() {
        return id;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public void setCategory(MenuCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPricePhp() {
        return pricePhp;
    }

    public void setPricePhp(BigDecimal pricePhp) {
        this.pricePhp = pricePhp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = (badgeLabel == null || badgeLabel.isBlank()) ? null : badgeLabel.trim();
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
