package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.InvalidCartStateException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        validateCustomerId(customerId);
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
        validateCustomerId(customerId);
        if (status == null) {
            throw new DomainValidationException("Cart status is required");
        }
        if (items != null && items.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("Cart items must not contain null elements");
        }
        this.id = id != null && !id.isBlank() ? id : UUID.randomUUID().toString();
        this.customerId = customerId;
        this.status = status;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        if (this.status == CartStatus.CHECKED_OUT && this.items.isEmpty()) {
            throw new InvalidCartStateException("Cannot check out an empty shopping cart");
        }
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.completedAt = completedAt;
        this.version = version;
    }

    public static ShoppingCart createNew(String customerId) {
        return new ShoppingCart(customerId);
    }

    public CartItem addItem(String offerId, ItemAction action, List<Price> prices) {
        ensureActive();
        CartItem item = new CartItem(offerId, action, prices);
        this.items.add(item);
        this.updatedAt = Instant.now();
        return item;
    }

    public void removeItem(String itemId) {
        ensureActive();
        if (itemId == null || itemId.isBlank()) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }
        boolean removed = this.items.removeIf(item -> item.itemId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }
        this.updatedAt = Instant.now();
    }

    public void evict() {
        ensureActive();
        Instant now = Instant.now();
        this.status = CartStatus.EVICTED;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void checkout() {
        ensureActive();
        if (this.items.isEmpty()) {
            throw new InvalidCartStateException("Cannot check out an empty shopping cart");
        }
        Instant now = Instant.now();
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = now;
        this.completedAt = now;
    }

    private void ensureActive() {
        if (this.status != CartStatus.ACTIVE) {
            throw new InvalidCartStateException("Shopping cart is not active: " + this.status);
        }
    }

    private static void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainValidationException("Customer ID must not be blank");
        }
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
