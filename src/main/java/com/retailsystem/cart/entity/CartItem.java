package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;

public record CartItem(
        String itemId,
        String offerId,
        ItemAction action,
        List<Price> prices,
        Instant addedAt
) {
}
