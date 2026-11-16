package com.retailsystem.cart.dto.write;

import com.retailsystem.cart.enums.ItemAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record OfferStatisticsQuery(
        @NotBlank(message = "Offer ID must not be blank")
        @Size(max = 128, message = "Offer ID exceeds maximum length of 128")
        String offerId,
        @NotNull(message = "Item action is required")
        ItemAction action,
        @NotNull(message = "Time range parameter 'from' is required")
        Instant from,
        @NotNull(message = "Time range parameter 'to' is required")
        Instant to,
        @Min(value = 1, message = "Retargeting limit must be between 1 and 50000")
        @Max(value = 50_000, message = "Retargeting limit must be between 1 and 50000")
        Integer retargetingLimit
) {

    public static final int DEFAULT_RETARGETING_LIMIT = 10_000;

    public OfferStatisticsQuery {
        if (retargetingLimit == null) {
            retargetingLimit = DEFAULT_RETARGETING_LIMIT;
        }
    }
}
