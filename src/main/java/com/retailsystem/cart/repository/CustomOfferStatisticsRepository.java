package com.retailsystem.cart.repository;

import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface CustomOfferStatisticsRepository {

    int DEFAULT_RETARGETING_LIMIT = 10_000;

    default OfferStatisticsResult aggregateOfferStatistics(String offerId, ItemAction action, Instant from, Instant to) {
        return aggregateOfferStatistics(offerId, action, from, to, DEFAULT_RETARGETING_LIMIT);
    }

    OfferStatisticsResult aggregateOfferStatistics(
            String offerId,
            ItemAction action,
            Instant from,
            Instant to,
            int retargetingLimit
    );

    record OfferStatisticsResult(
            long soldCount,
            long activeCartCount,
            long evictedCartCount,
            long totalAddedCount,
            List<String> retargetingCustomerIds
    ) {
        public OfferStatisticsResult {
            retargetingCustomerIds = retargetingCustomerIds != null
                    ? retargetingCustomerIds.stream().filter(Objects::nonNull).toList()
                    : List.of();
        }
    }
}
