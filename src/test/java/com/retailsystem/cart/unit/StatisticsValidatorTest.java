package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.validation.StatisticsValidator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatisticsValidatorTest {

    private final StatisticsValidator validator = new StatisticsValidator();

    @Test
    void shouldAcceptValidStatisticsQueryParameters() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-30T23:59:59Z");

        assertThatCode(() -> validator.validateQuery("tv-55", ItemAction.ADD, from, to))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateQuery("tv-55", ItemAction.ADD, from, from))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidStatisticsQueryParameters() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-30T23:59:59Z");

        assertThatThrownBy(() -> validator.validateQuery(null, ItemAction.ADD, from, to))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateQuery("   ", ItemAction.ADD, from, to))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateQuery("tv-55", null, from, to))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateQuery("tv-55", ItemAction.ADD, null, to))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateQuery("tv-55", ItemAction.ADD, from, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateQuery("tv-55", ItemAction.ADD, to, from))
                .isInstanceOf(DomainValidationException.class);
    }
}
