package com.jojo.integrationhub;

import com.jojo.integrationhub.event.Event;
import com.jojo.integrationhub.event.EventBus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EventBusTest {

    public void testSubscriberReceivesPublishedEvent() {
        EventBus bus = new EventBus();
        AtomicReference<Event> received = new AtomicReference<>();

        bus.subscribe("ORDER_RECEIVED", received::set);
        bus.publish(new Event("ORDER_RECEIVED", Map.of("orderId", "abc-123")));

        Assert.isTrue(received.get() != null, "Listener should have received the event");
        Assert.equals("abc-123", received.get().get("orderId"), "Payload should carry orderId");
    }

    public void testMultipleSubscribersAllReceiveTheEvent() {
        EventBus bus = new EventBus();
        AtomicInteger callCount = new AtomicInteger(0);

        bus.subscribe("ORDER_CONFIRMED", e -> callCount.incrementAndGet());
        bus.subscribe("ORDER_CONFIRMED", e -> callCount.incrementAndGet());
        bus.publish(new Event("ORDER_CONFIRMED", Map.of()));

        Assert.equals(2, callCount.get(), "Both subscribers should have fired");
    }

    public void testFailingListenerDoesNotStopOtherListeners() {
        EventBus bus = new EventBus();
        AtomicInteger secondListenerCalls = new AtomicInteger(0);

        bus.subscribe("ORDER_FAILED", e -> { throw new RuntimeException("boom"); });
        bus.subscribe("ORDER_FAILED", e -> secondListenerCalls.incrementAndGet());

        bus.publish(new Event("ORDER_FAILED", Map.of()));

        Assert.equals(1, secondListenerCalls.get(), "Second listener must still run despite the first throwing");
    }

    public void testUnsubscribedEventTypeIsSafeNoOp() {
        EventBus bus = new EventBus();
        // Should not throw even though nobody is listening for this type.
        bus.publish(new Event("SOME_UNKNOWN_EVENT", Map.of()));
    }
}
