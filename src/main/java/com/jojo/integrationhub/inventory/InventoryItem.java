package com.jojo.integrationhub.inventory;

public final class InventoryItem {

    private final String sku;
    private final String description;
    private int quantityOnHand;

    public InventoryItem(String sku, String description, int quantityOnHand) {
        this.sku = sku;
        this.description = description;
        this.quantityOnHand = quantityOnHand;
    }

    public String sku() {
        return sku;
    }

    public String description() {
        return description;
    }

    public int quantityOnHand() {
        return quantityOnHand;
    }

    public boolean reserve(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Reservation amount must be positive");
        }
        if (quantityOnHand < amount) {
            return false;
        }
        quantityOnHand -= amount;
        return true;
    }

    public String toCsvRow() {
        return sku + "," + description + "," + quantityOnHand;
    }

    public static InventoryItem fromCsvRow(String row) {
        String[] parts = row.split(",", 3);
        return new InventoryItem(parts[0], parts[1], Integer.parseInt(parts[2].trim()));
    }
}
