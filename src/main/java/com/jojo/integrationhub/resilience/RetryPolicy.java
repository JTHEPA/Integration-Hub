package com.jojo.integrationhub.resilience;

import com.jojo.integrationhub.util.SimpleLogger;

import java.util.concurrent.Callable;

/**
 * Fixed-attempt retry with linear backoff, used for transient failures
 * (e.g. a momentary file-lock contention or a flaky notification gateway).
 * Composed with {@link CircuitBreaker} at the call site: retries handle
 * short blips, the breaker handles sustained outages.
 */
public final class RetryPolicy {

    private static final SimpleLogger log = SimpleLogger.of(RetryPolicy.class);

    private final int maxAttempts;
    private final long backoffMillis;

    public RetryPolicy(int maxAttempts, long backoffMillis) {
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
    }

    public <T> T execute(String operationName, Callable<T> action) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastFailure = e;
                log.warn("Attempt " + attempt + "/" + maxAttempts + " for '" + operationName
                        + "' failed: " + e.getMessage());
                if (attempt < maxAttempts) {
                    Thread.sleep(backoffMillis * attempt);
                }
            }
        }
        throw lastFailure;
    }
}
