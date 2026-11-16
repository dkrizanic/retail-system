package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.OneTimePrice;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.RecurringPrice;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.RecurrenceUnit;
import com.retailsystem.cart.exception.DomainValidationException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceAndCartItemValidationTest {

    @Test
    void shouldCreateValidOneTimeAndRecurringPrices() {
        OneTimePrice oneTime = new OneTimePrice(new BigDecimal("499.99"));
        RecurringPrice recurring = new RecurringPrice(12, RecurrenceUnit.MONTHS, new BigDecimal("29.99"));

        assertThat(oneTime.value()).isEqualByComparingTo("499.99");
        assertThat(recurring.recurrences()).isEqualTo(12);
        assertThat(recurring.unit()).isEqualTo(RecurrenceUnit.MONTHS);
        assertThat(recurring.value()).isEqualByComparingTo("29.99");
    }

    @Test
    void shouldRejectInvalidOneTimePriceValues() {
        assertThatThrownBy(() -> new OneTimePrice(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new OneTimePrice(BigDecimal.ZERO))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new OneTimePrice(new BigDecimal("-10.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldRejectInvalidRecurringPriceAttributes() {
        assertThatThrownBy(() -> new RecurringPrice(0, RecurrenceUnit.MONTHS, new BigDecimal("29.99")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecurringPrice(-3, RecurrenceUnit.DAYS, new BigDecimal("29.99")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecurringPrice(12, null, new BigDecimal("29.99")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecurringPrice(12, RecurrenceUnit.YEARS, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecurringPrice(12, RecurrenceUnit.YEARS, BigDecimal.ZERO))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecurringPrice(12, RecurrenceUnit.YEARS, new BigDecimal("-1.00")))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldCreateValidCartItemWithMultiplePrices() {
        List<Price> prices = List.of(
                new OneTimePrice(new BigDecimal("19.99")),
                new RecurringPrice(24, RecurrenceUnit.MONTHS, new BigDecimal("34.99"))
        );

        CartItem item = CartItem.create("fiber-1gbps", ItemAction.MODIFY, prices);

        assertThat(item.itemId()).isNotBlank();
        assertThat(item.offerId()).isEqualTo("fiber-1gbps");
        assertThat(item.action()).isEqualTo(ItemAction.MODIFY);
        assertThat(item.prices()).hasSize(2);
        assertThat(item.addedAt()).isNotNull();
        assertThatThrownBy(() -> item.prices().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectCartItemWithMissingOrInvalidMandatoryAttributes() {
        Price validPrice = new OneTimePrice(new BigDecimal("99.99"));

        assertThatThrownBy(() -> CartItem.create(null, ItemAction.ADD, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CartItem.create("   ", ItemAction.ADD, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CartItem.create("tv-1", null, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CartItem.create("tv-1", ItemAction.ADD, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CartItem.create("tv-1", ItemAction.ADD, List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> CartItem.create("tv-1", ItemAction.ADD, Collections.singletonList(null)))
                .isInstanceOf(DomainValidationException.class);
    }
}
