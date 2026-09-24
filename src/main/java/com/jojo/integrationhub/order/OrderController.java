package com.jojo.integrationhub.order;

import com.jojo.integrationhub.util.SimpleLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin REST adapter over {@link OrderService}, built on the JDK's built-in
 * com.sun.net.httpserver.HttpServer so the whole project runs with zero
 * external dependencies. In a larger system this would be Spring MVC or
 * Javalin; the boundary here (controller only ever talks to the service
 * layer, never to inventory/notifications directly) is what should survive
 * that swap.
 *
 * Endpoints:
 *   POST /orders            body: sku=<sku>&quantity=<n>&email=<addr>
 *   GET  /orders/{id}
 *   GET  /health
 */
public final class OrderController {

    private static final SimpleLogger log = SimpleLogger.of(OrderController.class);
    private static final Pattern ORDER_ID_PATH = Pattern.compile("^/orders/([\\w-]+)$");

    private final OrderService orderService;
    private HttpServer server;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/orders", this::handleOrders);
        server.setExecutor(null); // default executor is fine for a demo
        server.start();
        log.info("OrderController listening on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "{\"status\":\"UP\",\"inventoryCircuit\":\""
                + orderService.inventoryCircuitState() + "\"}");
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equalsIgnoreCase(method) && "/orders".equals(path)) {
                handleCreateOrder(exchange);
                return;
            }
            Matcher m = ORDER_ID_PATH.matcher(path);
            if ("GET".equalsIgnoreCase(method) && m.matches()) {
                handleGetOrder(exchange, m.group(1));
                return;
            }
            respond(exchange, 404, "{\"error\":\"not found\"}");
        } catch (Exception e) {
            log.error("Unhandled error processing " + method + " " + path, e);
            respond(exchange, 500, "{\"error\":\"internal error\"}");
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);

        String sku = form.get("sku");
        String email = form.get("email");
        int quantity;
        try {
            quantity = Integer.parseInt(form.getOrDefault("quantity", "0"));
        } catch (NumberFormatException e) {
            quantity = 0;
        }

        if (sku == null || sku.isBlank() || email == null || email.isBlank() || quantity <= 0) {
            respond(exchange, 400, "{\"error\":\"sku, quantity (>0) and email are required\"}");
            return;
        }

        Order order = orderService.placeOrder(sku, quantity, email);
        int status = order.status() == OrderStatus.NOTIFIED ? 201 : 409;
        respond(exchange, status, order.toJson());
    }

    private void handleGetOrder(HttpExchange exchange, String orderId) throws IOException {
        Order order = orderService.find(orderId);
        if (order == null) {
            respond(exchange, 404, "{\"error\":\"order not found\"}");
            return;
        }
        respond(exchange, 200, order.toJson());
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    private void respond(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
