package com.retailsystem.cart.dto.read;

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
}
