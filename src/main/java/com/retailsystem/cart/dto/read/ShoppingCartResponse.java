package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.enums.CartStatus;
import java.time.Instant;
import java.util.List;

public record ShoppingCartResponse(
        String cartId,
        String customerId,
        CartStatus status,
        List<CartItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
