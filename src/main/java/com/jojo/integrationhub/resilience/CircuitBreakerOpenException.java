package com.jojo.integrationhub.resilience;

/** Thrown when a call is rejected because the circuit is currently open. */
public final class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String circuitName) {
        super("Circuit '" + circuitName + "' is open — call rejected fast");
    }
}
