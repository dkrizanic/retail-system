package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;

public record OfferStatisticsResponse(
        String offerId,
        ItemAction action,
        Instant from,
        Instant to,
        long soldCount,
        long activeCartCount,
        long evictedCartCount,
        long totalAddedCount,
        List<String> retargetingCustomerIds
) {
}
