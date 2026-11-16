package com.retailsystem.cart.dto.read;

import com.retailsystem.cart.enums.RecurrenceUnit;
import java.math.BigDecimal;

public record RecurringPriceReadDto(
        int recurrences,
        RecurrenceUnit unit,
        BigDecimal value
) implements PriceReadDto {
}
