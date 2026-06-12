package com.gubee.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
    * Armazena a linha do tempo (histórico) de todas as alterações aplicadas ao stock.
    * Garante a auditabilidade completa exigida pelo desafio.
 */
@Entity
@Table(name = "stock_history")
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String sku;

    @Column(name = "event_id", nullable = false)
    private String eventId; // Link de rastreabilidade com o evento originador

    @Column(nullable = false)
    private String type; // O tipo de operação executada

    @Column(name = "quantity_changed", nullable = false)
    private Integer quantityChanged; // Ex: -2 (venda), +2 (cancelamento)

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter; // Saldo final do stock após este evento

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public StockHistory() {}

    public StockHistory(String accountId, String sku, String eventId, String type, Integer quantityChanged, Integer balanceAfter, Instant occurredAt) {
        this.accountId = accountId;
        this.sku = sku;
        this.eventId = eventId;
        this.type = type;
        this.quantityChanged = quantityChanged;
        this.balanceAfter = balanceAfter;
        this.occurredAt = occurredAt;
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(Integer quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
