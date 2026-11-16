package com.retailsystem.cart.repository;

import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;

public interface CustomOfferStatisticsRepository {

    OfferStatisticsResult aggregateOfferStatistics(String offerId, ItemAction action, Instant from, Instant to);

    record OfferStatisticsResult(
            long soldCount,
            long activeCartCount,
            long evictedCartCount,
            long totalAddedCount,
            List<String> retargetingCustomerIds
    ) {
        public OfferStatisticsResult {
            retargetingCustomerIds = retargetingCustomerIds != null ? List.copyOf(retargetingCustomerIds) : List.of();
        }
    }
}
