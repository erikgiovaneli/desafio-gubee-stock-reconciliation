package com.gubee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registra todos os eventos recebidos pela API REST.
 * O eventId é utilizado como Chave Primária (Primary Key) para garantir
 * a idempotência do processamento diretamente ao nível do banco de dados.
 */
@Entity
@Table(name = "event_store")
public class EventStore {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private String marketplace;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(nullable = false)
    private String sku;

    private Integer quantity;

    private Integer available;

    private String reason;

    @Column(name = "quantity_sent")
    private Integer quantitySent;

    @Column(nullable = false)
    private String status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    public EventStore() {}

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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(String marketplace) {
        this.marketplace = marketplace;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
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

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getQuantitySent() {
        return quantitySent;
    }

    public void setQuantitySent(Integer quantitySent) {
        this.quantitySent = quantitySent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
