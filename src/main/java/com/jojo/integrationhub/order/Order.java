package com.jojo.integrationhub.order;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain model for an order. Deliberately framework-free (no
 * annotations) so it can be reused by the REST layer, the event payloads,
 * and tests without dragging in any particular framework's assumptions.
 */
public final class Order {

    private final String id;
    private final String sku;
    private final int quantity;
    private final String customerEmail;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(String sku, int quantity, String customerEmail) {
        this.id = UUID.randomUUID().toString();
        this.sku = sku;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
        this.createdAt = Instant.now();
        this.status = OrderStatus.RECEIVED;
    }

    public String id() {
        return id;
    }

    public String sku() {
        return sku;
    }

    public int quantity() {
        return quantity;
    }

    public String customerEmail() {
        return customerEmail;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public OrderStatus status() {
        return status;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    /** Hand-rolled JSON so the project has zero JSON-library dependency. */
    public String toJson() {
        return """
                {"id":"%s","sku":"%s","quantity":%d,"customerEmail":"%s","status":"%s","createdAt":"%s"}"""
                .formatted(id, sku, quantity, customerEmail, status, createdAt);
    }
}
