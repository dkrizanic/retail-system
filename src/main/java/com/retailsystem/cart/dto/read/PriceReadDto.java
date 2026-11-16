package com.retailsystem.cart.dto.read;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.enums.PriceType;
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

    static PriceReadDto from(Price price) {
        if (price.type() == PriceType.RECURRING) {
            return new RecurringPriceReadDto(price.recurrences(), price.unit(), price.value());
        }
        return new OneTimePriceReadDto(price.value());
    }
}
