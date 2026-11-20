package com.ilonggobistro.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class MenuItemForm {

    @NotNull(message = "Please choose a category")
    private Long categoryId;

    @NotBlank(message = "Dish name is required")
    @Size(max = 140, message = "Dish name must be 140 characters or fewer")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be 1000 characters or fewer")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1.00", message = "Price must be at least ₱1.00")
    @Digits(integer = 8, fraction = 2, message = "Enter a valid PHP price")
    private BigDecimal pricePhp;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 40)
    private String badgeLabel;

    private boolean featured;
    private boolean available = true;
    private int displayOrder;

    public MenuItemForm() {
    }

    public static MenuItemForm fromEntity(MenuItem item) {
        MenuItemForm form = new MenuItemForm();
        form.setCategoryId(item.getCategory().getId());
        form.setName(item.getName());
        form.setDescription(item.getDescription());
        form.setPricePhp(item.getPricePhp());
        form.setImageUrl(item.getImageUrl());
        form.setBadgeLabel(item.getBadgeLabel());
        form.setFeatured(item.isFeatured());
        form.setAvailable(item.isAvailable());
        form.setDisplayOrder(item.getDisplayOrder());
        return form;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
        this.badgeLabel = badgeLabel;
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
}
