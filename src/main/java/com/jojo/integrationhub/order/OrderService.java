package com.jojo.integrationhub.order;

import com.jojo.integrationhub.event.Event;
import com.jojo.integrationhub.event.EventBus;
import com.jojo.integrationhub.inventory.InventoryRepository;
import com.jojo.integrationhub.notification.NotificationService;
import com.jojo.integrationhub.resilience.CircuitBreaker;
import com.jojo.integrationhub.resilience.RetryPolicy;
import com.jojo.integrationhub.util.SimpleLogger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The orchestration hub of the whole demo: this is where the REST layer,
 * the file-backed inventory system, the notification channels, and the
 * event bus all meet.
 *
 * Flow for a new order:
 *   1. Persist the order in memory and publish ORDER_RECEIVED.
 *   2. Try to reserve stock in the inventory system, protected by a
 *      circuit breaker + retry policy (inventory is file I/O and can
 *      transiently fail).
 *   3. On success, notify the customer and publish ORDER_CONFIRMED.
 *   4. On failure (no stock, or inventory system unavailable), notify the
 *      customer of the failure and publish ORDER_FAILED.
 */
public final class OrderService {

    private static final SimpleLogger log = SimpleLogger.of(OrderService.class);

    private final InventoryRepository inventory;
    private final NotificationService notifications;
    private final EventBus eventBus;
    private final CircuitBreaker inventoryBreaker;
    private final RetryPolicy retryPolicy;

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public OrderService(InventoryRepository inventory,
                         NotificationService notifications,
                         EventBus eventBus) {
        this.inventory = inventory;
        this.notifications = notifications;
        this.eventBus = eventBus;
        this.inventoryBreaker = new CircuitBreaker("inventory", 3, Duration.ofSeconds(10));
        this.retryPolicy = new RetryPolicy(3, 100);
    }

    public Order placeOrder(String sku, int quantity, String customerEmail) {
        Order order = new Order(sku, quantity, customerEmail);
        orders.put(order.id(), order);
        eventBus.publish(new Event("ORDER_RECEIVED", Map.of(
                "orderId", order.id(), "sku", sku, "quantity", quantity)));

        boolean reserved;
        try {
            reserved = retryPolicy.execute("reserve-stock",
                    () -> inventoryBreaker.call(() -> inventory.reserve(sku, quantity)));
        } catch (Exception e) {
            log.error("Inventory system unavailable for order " + order.id(), e);
            reserved = false;
        }

        if (reserved) {
            order.updateStatus(OrderStatus.STOCK_RESERVED);
            notifications.notifyOrderConfirmed(customerEmail, order.id(), sku);
            order.updateStatus(OrderStatus.NOTIFIED);
            eventBus.publish(new Event("ORDER_CONFIRMED", Map.of(
                    "orderId", order.id(), "sku", sku)));
        } else {
            order.updateStatus(OrderStatus.STOCK_UNAVAILABLE);
            notifications.notifyOrderFailed(customerEmail, order.id(),
                    "Item " + sku + " is currently unavailable.");
            eventBus.publish(new Event("ORDER_FAILED", Map.of(
                    "orderId", order.id(), "sku", sku, "reason", "stock_unavailable")));
        }

        return order;
    }

    public Order find(String orderId) {
        return orders.get(orderId);
    }

    public CircuitBreaker.State inventoryCircuitState() {
        return inventoryBreaker.state();
    }
}
