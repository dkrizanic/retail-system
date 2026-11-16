package com.retailsystem.cart.dto.read;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.retailsystem.cart.entity.OneTimePrice;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.RecurringPrice;
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
        return switch (price) {
            case OneTimePrice oneTime -> new OneTimePriceReadDto(oneTime.value());
            case RecurringPrice recurring -> new RecurringPriceReadDto(
                    recurring.recurrences(),
                    recurring.unit(),
                    recurring.value()
            );
        };
    }
}
