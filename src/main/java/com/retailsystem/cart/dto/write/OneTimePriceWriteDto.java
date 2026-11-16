package com.retailsystem.cart.dto.write;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OneTimePriceWriteDto(
        @NotNull(message = "Price value is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "One-time price value must be greater than zero")
        BigDecimal value
) implements PriceWriteDto {
}
