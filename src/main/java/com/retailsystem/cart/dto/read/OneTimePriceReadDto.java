package com.retailsystem.cart.dto.read;

import java.math.BigDecimal;

public record OneTimePriceReadDto(BigDecimal value) implements PriceReadDto {
}
