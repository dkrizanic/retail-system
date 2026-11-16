package com.retailsystem.cart.validation;

import com.retailsystem.cart.exception.DomainValidationException;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class StatisticsValidator {

    public void validateTimeRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new DomainValidationException("Start timestamp 'from' must not be after 'to'");
        }
    }
}
