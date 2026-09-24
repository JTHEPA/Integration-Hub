package com.jojo.integrationhub.notification;

import com.jojo.integrationhub.util.SimpleLogger;

/**
 * Stand-in adapter for a real SMS gateway (Twilio, Clickatell, etc.).
 * Kept separate from {@link EmailNotificationAdapter} to show the same
 * NotificationChannel contract serving two unrelated external systems.
 */
public final class SmsNotificationAdapter implements NotificationChannel {

    private static final SimpleLogger log = SimpleLogger.of(SmsNotificationAdapter.class);

    @Override
    public void send(String recipient, String subject, String message) {
        // In production this would call an SMS gateway's API.
        log.info("SMS -> " + recipient + " | " + subject + ": " + message);
    }

    @Override
    public String channelName() {
        return "sms";
    }
}
