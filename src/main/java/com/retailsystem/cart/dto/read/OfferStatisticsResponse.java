package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository.OfferStatisticsResult;
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

    public OfferStatisticsResponse {
        retargetingCustomerIds = retargetingCustomerIds != null ? List.copyOf(retargetingCustomerIds) : List.of();
    }

    public static OfferStatisticsResponse from(
            String offerId,
            ItemAction action,
            Instant from,
            Instant to,
            OfferStatisticsResult result
    ) {
        return new OfferStatisticsResponse(
                offerId,
                action,
                from,
                to,
                result.soldCount(),
                result.activeCartCount(),
                result.evictedCartCount(),
                result.totalAddedCount(),
                result.retargetingCustomerIds()
        );
    }
}
