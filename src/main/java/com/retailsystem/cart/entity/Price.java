package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.enums.RecurrenceUnit;
import java.math.BigDecimal;

public record Price(
        PriceType type,
        BigDecimal value,
        Integer recurrences,
        RecurrenceUnit unit
) {

    public static Price oneTime(BigDecimal value) {
        return new Price(PriceType.ONE_TIME, value, null, null);
    }

    public static Price recurring(int recurrences, RecurrenceUnit unit, BigDecimal value) {
        return new Price(PriceType.RECURRING, value, recurrences, unit);
    }
}
