package com.retailsystem.cart.dto.write;

import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.enums.RecurrenceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceWriteDto(
        @NotNull(message = "Price type is required")
        PriceType type,
        @NotNull(message = "Price value is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price value must be greater than zero")
        BigDecimal value,
        Integer recurrences,
        RecurrenceUnit unit
) {
}
