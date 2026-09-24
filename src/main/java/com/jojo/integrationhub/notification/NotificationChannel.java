package com.jojo.integrationhub.notification;

/**
 * The Adapter-pattern seam for outbound notifications. Each real-world
 * channel (email, SMS, push, Slack...) implements this the same way, so
 * {@link NotificationService} can treat them interchangeably and new
 * channels can be added without touching existing ones.
 */
public interface NotificationChannel {
    void send(String recipient, String subject, String message);

    String channelName();
}
