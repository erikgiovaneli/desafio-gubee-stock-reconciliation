package com.gubee.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Representa o saldo consolidado de estoque atual por conta e SKU.
 * A unicidade é garantida pela combinação de accountId e sku.
 */
@Entity
@Table(name = "stock_balance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"accountId","sku"})
})
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Version
    private Long version; // Controle de Concorrência Otimista (Optimistic Locking) para evitar condições de corrida

    public StockBalance() {}

    public StockBalance(String accountId, String sku, Integer quantity, Instant lastUpdated) {
        this.accountId = accountId;
        this.sku = sku;
        this.quantity = quantity;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
