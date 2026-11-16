package com.retailsystem.cart.dto.write;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OneTimePriceWriteDto.class, name = "ONE_TIME"),
        @JsonSubTypes.Type(value = RecurringPriceWriteDto.class, name = "RECURRING")
})
public sealed interface PriceWriteDto permits OneTimePriceWriteDto, RecurringPriceWriteDto {

    BigDecimal value();
}
