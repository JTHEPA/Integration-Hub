package com.jojo.integrationhub.event;

/**
 * Anything that reacts to events published on the {@link EventBus}.
 * Kept as a functional interface so subsystems can subscribe with a
 * one-line lambda instead of a whole class.
 */
@FunctionalInterface
public interface EventListener {
    void onEvent(Event event);
}
