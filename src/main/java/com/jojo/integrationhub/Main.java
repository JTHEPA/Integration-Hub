package com.jojo.integrationhub;

import com.jojo.integrationhub.event.EventBus;
import com.jojo.integrationhub.inventory.InventoryRepository;
import com.jojo.integrationhub.notification.EmailNotificationAdapter;
import com.jojo.integrationhub.notification.NotificationService;
import com.jojo.integrationhub.notification.SmsNotificationAdapter;
import com.jojo.integrationhub.order.OrderController;
import com.jojo.integrationhub.order.OrderService;
import com.jojo.integrationhub.util.SimpleLogger;

import java.nio.file.Path;
import java.util.List;

/**
 * Composition root: this is the only class that knows about every concrete
 * implementation in the system. Everything downstream of here is wired
 * against interfaces, which is what keeps each subsystem independently
 * testable (see src/test).
 */
public final class Main {

    private static final SimpleLogger log = SimpleLogger.of(Main.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path inventoryFile = Path.of("data", "inventory.csv");

        EventBus eventBus = new EventBus();

        InventoryRepository inventory = new InventoryRepository(inventoryFile);

        NotificationService notifications = new NotificationService(List.of(
                new EmailNotificationAdapter(),
                new SmsNotificationAdapter()
        ));

        eventBus.subscribe("ORDER_RECEIVED", e -> log.info("[audit] order received: " + e.get("orderId")));
        eventBus.subscribe("ORDER_CONFIRMED", e -> log.info("[audit] order confirmed: " + e.get("orderId")));
        eventBus.subscribe("ORDER_FAILED", e -> log.info("[audit] order failed: " + e.get("orderId")));

        OrderService orderService = new OrderService(inventory, notifications, eventBus);
        OrderController controller = new OrderController(orderService);
        controller.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Integration Hub...");
            controller.stop();
        }));

        log.info("Integration Hub is up. Try:");
        log.info("  curl http://localhost:" + port + "/health");
        log.info("  curl -X POST http://localhost:" + port
                + "/orders -d 'sku=SKU-1001&quantity=2&email=customer@example.com'");
    }
}
