package com.jojo.integrationhub.notification;

import com.jojo.integrationhub.util.SimpleLogger;

import java.util.List;

/**
 * Fans a single notification out across every registered
 * {@link NotificationChannel}. This is the piece an OrderService talks to;
 * it never needs to know whether that ends up being one channel or five.
 */
public final class NotificationService {

    private static final SimpleLogger log = SimpleLogger.of(NotificationService.class);

    private final List<NotificationChannel> channels;

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = List.copyOf(channels);
    }

    public void notifyOrderConfirmed(String recipient, String orderId, String sku) {
        String subject = "Order " + orderId + " confirmed";
        String message = "Your order for " + sku + " has been confirmed and stock reserved.";
        dispatch(recipient, subject, message);
    }

    public void notifyOrderFailed(String recipient, String orderId, String reason) {
        String subject = "Order " + orderId + " could not be completed";
        dispatch(recipient, subject, reason);
    }

    private void dispatch(String recipient, String subject, String message) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(recipient, subject, message);
            } catch (Exception e) {
                // One channel failing (e.g. SMS gateway down) must not stop
                // the others from delivering the same notification.
                log.error("Channel '" + channel.channelName() + "' failed to send", e);
            }
        }
    }
}
