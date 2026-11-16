package com.retailsystem.cart.dto.write;

import com.retailsystem.cart.enums.RecurrenceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecurringPriceWriteDto(
        @Min(value = 1, message = "Recurrences must be greater than zero")
        int recurrences,
        @NotNull(message = "Recurrence unit is required")
        RecurrenceUnit unit,
        @NotNull(message = "Price value is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Recurring price value must be greater than zero")
        BigDecimal value
) implements PriceWriteDto {
}
