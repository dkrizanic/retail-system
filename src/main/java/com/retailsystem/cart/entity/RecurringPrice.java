package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.RecurrenceUnit;
import com.retailsystem.cart.exception.DomainValidationException;
import java.math.BigDecimal;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("RECURRING")
public record RecurringPrice(int recurrences, RecurrenceUnit unit, BigDecimal value) implements Price {

    public RecurringPrice {
        if (recurrences <= 0) {
            throw new DomainValidationException("Recurrences must be greater than zero");
        }
        if (unit == null) {
            throw new DomainValidationException("Recurrence unit is required");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Recurring price value must be greater than zero");
        }
    }
}
