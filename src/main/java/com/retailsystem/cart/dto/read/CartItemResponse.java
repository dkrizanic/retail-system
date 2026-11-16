package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;

public record CartItemResponse(
        String itemId,
        String offerId,
        ItemAction action,
        List<PriceReadDto> prices,
        Instant addedAt
) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.itemId(),
                item.offerId(),
                item.action(),
                item.prices().stream().map(PriceReadDto::from).toList(),
                item.addedAt()
        );
    }
}
