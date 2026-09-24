package com.jojo.integrationhub.resilience;

import com.jojo.integrationhub.util.SimpleLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A minimal three-state circuit breaker (CLOSED -> OPEN -> HALF_OPEN)
 * protecting a downstream integration point.
 *
 * This exists because every external system this project touches
 * (inventory file I/O, notification channels) can fail or slow down.
 * Without a breaker, repeated calls to a failing dependency waste time and
 * can cascade the failure back to the caller (here: the HTTP API). Once the
 * failure threshold trips, calls fail fast until a cool-down elapses and a
 * single trial call is allowed through.
 */
public final class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private static final SimpleLogger log = SimpleLogger.of(CircuitBreaker.class);

    private final String name;
    private final int failureThreshold;
    private final Duration coolDown;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt = Instant.EPOCH;

    public CircuitBreaker(String name, int failureThreshold, Duration coolDown) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.coolDown = coolDown;
    }

    public <T> T call(Callable<T> action) throws Exception {
        if (state.get() == State.OPEN) {
            if (Duration.between(openedAt, Instant.now()).compareTo(coolDown) >= 0) {
                state.set(State.HALF_OPEN);
                log.info("Circuit '" + name + "' moving OPEN -> HALF_OPEN (trial call)");
            } else {
                throw new CircuitBreakerOpenException(name);
            }
        }

        try {
            T result = action.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
            log.info("Circuit '" + name + "' recovered: HALF_OPEN -> CLOSED");
        }
    }

    private void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (state.get() == State.HALF_OPEN || failures >= failureThreshold) {
            state.set(State.OPEN);
            openedAt = Instant.now();
            log.warn("Circuit '" + name + "' tripped OPEN after " + failures + " consecutive failures");
        }
    }

    public State state() {
        return state.get();
    }

    public String name() {
        return name;
    }
}
