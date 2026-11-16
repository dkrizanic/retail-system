package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.entity.ShoppingCart;
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

    public static ShoppingCartResponse from(ShoppingCart cart) {
        return new ShoppingCartResponse(
                cart.getId(),
                cart.getCustomerId(),
                cart.getStatus(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                cart.getCompletedAt()
        );
    }
}
