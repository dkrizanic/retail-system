package com.retailsystem.cart.dto.read;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OneTimePriceReadDto.class, name = "ONE_TIME"),
        @JsonSubTypes.Type(value = RecurringPriceReadDto.class, name = "RECURRING")
})
public sealed interface PriceReadDto permits OneTimePriceReadDto, RecurringPriceReadDto {

    BigDecimal value();
}
