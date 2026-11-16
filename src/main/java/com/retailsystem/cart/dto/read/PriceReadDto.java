package com.retailsystem.cart.dto.read;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.enums.RecurrenceUnit;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceReadDto(
        PriceType type,
        BigDecimal value,
        Integer recurrences,
        RecurrenceUnit unit
) {
}
