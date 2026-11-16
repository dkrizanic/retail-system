package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartItem(
        String itemId,
        String offerId,
        ItemAction action,
        List<Price> prices,
        Instant addedAt
) {

    public CartItem {
        prices = prices != null ? List.copyOf(prices) : List.of();
    }

    public static CartItem create(String offerId, ItemAction action, List<Price> prices) {
        return new CartItem(UUID.randomUUID().toString(), offerId, action, prices, Instant.now());
    }
}
