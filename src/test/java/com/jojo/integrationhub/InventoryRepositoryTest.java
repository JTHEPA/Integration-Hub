package com.jojo.integrationhub;

import com.jojo.integrationhub.inventory.InventoryRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class InventoryRepositoryTest {

    private Path newTestCsv() throws Exception {
        Path tmp = Files.createTempFile("inventory-test", ".csv");
        Files.writeString(tmp, """
                SKU-TEST-1,Test Widget,5
                SKU-TEST-2,Rare Gadget,1
                """, StandardCharsets.UTF_8);
        return tmp;
    }

    public void testFindBySkuReturnsLoadedItem() throws Exception {
        InventoryRepository repo = new InventoryRepository(newTestCsv());
        Assert.isTrue(repo.findBySku("SKU-TEST-1").isPresent(), "Should find item loaded from CSV");
        Assert.equals(5, repo.findBySku("SKU-TEST-1").get().quantityOnHand(), "Quantity should match CSV");
    }

    public void testReserveSucceedsAndDecrementsStock() throws Exception {
        InventoryRepository repo = new InventoryRepository(newTestCsv());
        boolean reserved = repo.reserve("SKU-TEST-1", 3);
        Assert.isTrue(reserved, "Reservation within stock should succeed");
        Assert.equals(2, repo.findBySku("SKU-TEST-1").get().quantityOnHand(), "Stock should decrement by reserved amount");
    }

    public void testReserveFailsWhenInsufficientStock() throws Exception {
        InventoryRepository repo = new InventoryRepository(newTestCsv());
        boolean reserved = repo.reserve("SKU-TEST-2", 10);
        Assert.isFalse(reserved, "Reservation exceeding stock should fail");
        Assert.equals(1, repo.findBySku("SKU-TEST-2").get().quantityOnHand(), "Stock should be unchanged on failed reservation");
    }

    public void testReserveUnknownSkuReturnsFalse() throws Exception {
        InventoryRepository repo = new InventoryRepository(newTestCsv());
        Assert.isFalse(repo.reserve("SKU-DOES-NOT-EXIST", 1), "Unknown SKU should not be reservable");
    }
}
