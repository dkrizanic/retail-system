package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.validation.StatisticsValidator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatisticsValidatorTest {

    private final StatisticsValidator validator = new StatisticsValidator();

    @Test
    void shouldAcceptValidAndRejectInvertedTimeRange() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-30T23:59:59Z");

        assertThatCode(() -> validator.validateTimeRange(from, to)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateTimeRange(from, from)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateTimeRange(to, from))
                .isInstanceOf(DomainValidationException.class);
    }
}
