package com.retailsystem.cart.validation;

import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class StatisticsValidator {

    public void validateQuery(String offerId, ItemAction action, Instant from, Instant to) {
        if (offerId == null || offerId.isBlank()) {
            throw new DomainValidationException("Offer ID must not be blank");
        }
        if (action == null) {
            throw new DomainValidationException("Item action is required");
        }
        if (from == null || to == null) {
            throw new DomainValidationException("Time range parameters 'from' and 'to' are required");
        }
        if (from.isAfter(to)) {
            throw new DomainValidationException("Start timestamp 'from' must not be after 'to'");
        }
    }
}
