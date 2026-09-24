package com.jojo.integrationhub.event;

import java.time.Instant;
import java.util.Map;

/**
 * A single event flowing through the {@link EventBus}.
 *
 * Events are the contract between subsystems (order intake, inventory,
 * notifications). Any subsystem can publish an event without knowing who —
 * if anyone — is listening. This is what lets us add or remove integrations
 * without touching the systems that already exist.
 */
public final class Event {

    private final String type;
    private final Instant occurredAt;
    private final Map<String, Object> payload;

    public Event(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = Map.copyOf(payload);
        this.occurredAt = Instant.now();
    }

    public String type() {
        return type;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public Object get(String key) {
        return payload.get(key);
    }

    @Override
    public String toString() {
        return "Event{type='%s', occurredAt=%s, payload=%s}"
                .formatted(type, occurredAt, payload);
    }
}
