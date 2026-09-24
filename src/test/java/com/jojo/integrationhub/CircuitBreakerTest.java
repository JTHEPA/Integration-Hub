package com.jojo.integrationhub;

import com.jojo.integrationhub.resilience.CircuitBreaker;
import com.jojo.integrationhub.resilience.CircuitBreakerOpenException;

import java.time.Duration;

public class CircuitBreakerTest {

    public void testStaysClosedOnSuccess() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("test", 3, Duration.ofMillis(50));
        cb.call(() -> "ok");
        Assert.equals(CircuitBreaker.State.CLOSED, cb.state(), "Should remain CLOSED after a success");
    }

    public void testTripsOpenAfterThresholdFailures() {
        CircuitBreaker cb = new CircuitBreaker("test", 2, Duration.ofSeconds(30));

        for (int i = 0; i < 2; i++) {
            try {
                cb.call(() -> { throw new RuntimeException("fail"); });
            } catch (Exception ignored) {
                // expected
            }
        }

        Assert.equals(CircuitBreaker.State.OPEN, cb.state(), "Should be OPEN after reaching the failure threshold");
    }

    public void testRejectsFastWhileOpen() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("test", 1, Duration.ofSeconds(30));
        try {
            cb.call(() -> { throw new RuntimeException("fail"); });
        } catch (Exception ignored) {
            // expected, trips the breaker
        }

        boolean rejectedFast = false;
        try {
            cb.call(() -> "should not run");
        } catch (CircuitBreakerOpenException e) {
            rejectedFast = true;
        }
        Assert.isTrue(rejectedFast, "Calls while OPEN should fail fast with CircuitBreakerOpenException");
    }

    public void testRecoversAfterCoolDown() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("test", 1, Duration.ofMillis(20));
        try {
            cb.call(() -> { throw new RuntimeException("fail"); });
        } catch (Exception ignored) {
            // trips it open
        }

        Thread.sleep(30); // wait out the cool-down
        String result = cb.call(() -> "recovered");

        Assert.equals("recovered", result, "Trial call after cool-down should succeed");
        Assert.equals(CircuitBreaker.State.CLOSED, cb.state(), "Should close again after a successful trial call");
    }
}
