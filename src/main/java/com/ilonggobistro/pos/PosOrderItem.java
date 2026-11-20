package com.ilonggobistro.pos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "pos_order_items")
public class PosOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PosOrder order;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "item_name", nullable = false, length = 140)
    private String itemName;

    @Column(name = "unit_price_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPricePhp;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_php", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotalPhp;

    protected PosOrderItem() {
    }

    public PosOrderItem(Long menuItemId, String itemName, BigDecimal unitPricePhp, int quantity, BigDecimal lineTotalPhp) {
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.unitPricePhp = unitPricePhp;
        this.quantity = quantity;
        this.lineTotalPhp = lineTotalPhp;
    }

    void setOrder(PosOrder order) {
        this.order = order;
    }

    public String formattedUnitPrice() {
        return PosOrder.formatPhp(unitPricePhp);
    }

    public String formattedLineTotal() {
        return PosOrder.formatPhp(lineTotalPhp);
    }

    public Long getId() {
        return id;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getUnitPricePhp() {
        return unitPricePhp;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotalPhp() {
        return lineTotalPhp;
    }
}
