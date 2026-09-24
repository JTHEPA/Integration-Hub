package com.jojo.integrationhub.event;

import com.jojo.integrationhub.util.SimpleLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A small in-memory publish/subscribe bus that decouples the subsystems
 * integrated by this project (order intake, inventory, notifications).
 *
 * In a production system this would be backed by Kafka/RabbitMQ/SQS; the
 * public API here (subscribe/publish) is intentionally shaped so that a
 * real broker client could be dropped in later without changing any
 * calling code — that substitutability is the integration pattern being
 * demonstrated.
 */
public final class EventBus {

    private static final SimpleLogger log = SimpleLogger.of(EventBus.class);

    private final Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.info("Subscribed a listener to '" + eventType + "'");
    }

    public void publish(Event event) {
        log.info("Publishing " + event);
        List<EventListener> subscribers = listeners.getOrDefault(event.type(), List.of());
        if (subscribers.isEmpty()) {
            log.warn("No subscribers for event type '" + event.type() + "'");
            return;
        }
        for (EventListener listener : subscribers) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // One misbehaving subscriber must never take down the bus
                // or the other subscribers — this is the isolation
                // guarantee integration code needs to rely on.
                log.error("Listener threw while handling " + event.type(), e);
            }
        }
    }
}
