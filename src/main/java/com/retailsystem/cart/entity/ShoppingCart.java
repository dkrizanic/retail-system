package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.CartStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shopping_carts")
public class ShoppingCart {

    @Id
    private String id;
    private String customerId;
    private CartStatus status;
    private List<CartItem> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    @Version
    private Long version;

    public ShoppingCart(String customerId) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.status = CartStatus.ACTIVE;
        this.items = new ArrayList<>();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PersistenceCreator
    public ShoppingCart(
            String id,
            String customerId,
            CartStatus status,
            List<CartItem> items,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            Long version
    ) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.version = version;
    }

    public static ShoppingCart createNew(String customerId) {
        return new ShoppingCart(customerId);
    }

    public void addItem(CartItem item) {
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public boolean removeItemById(String itemId) {
        boolean removed = this.items.removeIf(item -> item.itemId().equals(itemId));
        if (removed) {
            this.updatedAt = Instant.now();
        }
        return removed;
    }

    public void markEvicted() {
        Instant now = Instant.now();
        this.status = CartStatus.EVICTED;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void markCheckedOut() {
        Instant now = Instant.now();
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }
}
