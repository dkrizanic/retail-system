package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.PersistenceCreator;

public record CartItem(
        String itemId,
        String offerId,
        ItemAction action,
        List<Price> prices,
        Instant addedAt
) {

    @PersistenceCreator
    public CartItem {
        if (itemId == null || itemId.isBlank()) {
            throw new DomainValidationException("Item ID must not be blank");
        }
        if (offerId == null || offerId.isBlank()) {
            throw new DomainValidationException("Offer ID must not be blank");
        }
        if (action == null) {
            throw new DomainValidationException("Item action is required");
        }
        if (prices == null || prices.isEmpty() || prices.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("Cart item must contain at least one valid price");
        }
        if (addedAt == null) {
            throw new DomainValidationException("Added timestamp is required");
        }
        prices = List.copyOf(prices);
    }

    public CartItem(String offerId, ItemAction action, List<Price> prices) {
        this(UUID.randomUUID().toString(), offerId, action, prices, Instant.now());
    }

    public static CartItem create(String offerId, ItemAction action, List<Price> prices) {
        return new CartItem(offerId, action, prices);
    }
}
