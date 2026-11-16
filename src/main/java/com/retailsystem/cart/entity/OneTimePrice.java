package com.retailsystem.cart.entity;

import com.retailsystem.cart.exception.DomainValidationException;
import java.math.BigDecimal;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ONE_TIME")
public record OneTimePrice(BigDecimal value) implements Price {

    public OneTimePrice {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("One-time price value must be greater than zero");
        }
    }
}
