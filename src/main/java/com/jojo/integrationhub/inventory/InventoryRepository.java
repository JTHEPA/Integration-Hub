package com.jojo.integrationhub.inventory;

import com.jojo.integrationhub.util.SimpleLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Integrates with the "legacy" inventory system, modelled here as a
 * CSV file (data/inventory.csv) to stand in for a system this project does
 * not control the schema of. All access goes through this repository so the
 * rest of the codebase never has to know the source is a flat file — it
 * could become a JDBC datasource or an HTTP call to a warehouse system
 * later with no change to callers.
 *
 * Reads are cached in memory; writes go straight back to disk, keeping the
 * file as the single source of truth (important for a system that other,
 * non-Java tools might also read).
 */
public final class InventoryRepository {

    private static final SimpleLogger log = SimpleLogger.of(InventoryRepository.class);

    private final Path csvPath;
    private final Map<String, InventoryItem> cache = new ConcurrentHashMap<>();

    public InventoryRepository(Path csvPath) {
        this.csvPath = csvPath;
        load();
    }

    private void load() {
        try {
            if (!Files.exists(csvPath)) {
                log.warn("Inventory file not found at " + csvPath + "; starting empty");
                return;
            }
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                InventoryItem item = InventoryItem.fromCsvRow(line);
                cache.put(item.sku(), item);
            }
            log.info("Loaded " + cache.size() + " inventory items from " + csvPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load inventory file: " + csvPath, e);
        }
    }

    public Optional<InventoryItem> findBySku(String sku) {
        return Optional.ofNullable(cache.get(sku));
    }

    public List<InventoryItem> findAll() {
        return List.copyOf(cache.values());
    }

    /**
     * Attempts to reserve stock for an order. Returns true if the reservation
     * succeeded and the change has been persisted back to the CSV file.
     */
    public synchronized boolean reserve(String sku, int quantity) {
        InventoryItem item = cache.get(sku);
        if (item == null) {
            log.warn("Reserve failed: unknown SKU '" + sku + "'");
            return false;
        }
        boolean reserved = item.reserve(quantity);
        if (reserved) {
            persist();
            log.info("Reserved " + quantity + " of '" + sku + "' (" + item.quantityOnHand() + " remaining)");
        } else {
            log.warn("Reserve failed: insufficient stock for '" + sku + "' (have "
                    + item.quantityOnHand() + ", wanted " + quantity + ")");
        }
        return reserved;
    }

    private void persist() {
        try {
            String content = cache.values().stream()
                    .map(InventoryItem::toCsvRow)
                    .collect(Collectors.joining(System.lineSeparator()));
            Files.writeString(csvPath, content + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist inventory file: " + csvPath, e);
        }
    }
}
