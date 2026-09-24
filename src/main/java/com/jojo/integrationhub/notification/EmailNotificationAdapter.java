package com.jojo.integrationhub.notification;

import com.jojo.integrationhub.util.SimpleLogger;

/**
 * Stand-in adapter for a real email provider (SES, SendGrid, etc.).
 * Swapping the "send" implementation for a real HTTP call is the only
 * change needed to go from demo to production — nothing else in the
 * system depends on how the email is actually delivered.
 */
public final class EmailNotificationAdapter implements NotificationChannel {

    private static final SimpleLogger log = SimpleLogger.of(EmailNotificationAdapter.class);

    @Override
    public void send(String recipient, String subject, String message) {
        // In production this would call an email provider's API.
        log.info("EMAIL -> " + recipient + " | subject='" + subject + "' | " + message);
    }

    @Override
    public String channelName() {
        return "email";
    }
}
