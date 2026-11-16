package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.enums.RecurrenceUnit;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.InvalidCartStateException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import com.retailsystem.cart.util.CartTestFixtures;
import com.retailsystem.cart.validation.CartValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartValidatorTest {

    private final CartValidator validator = new CartValidator();

    @Test
    void shouldAcceptValidOneTimeAndRecurringPrices() {
        Price oneTime = Price.oneTime(new BigDecimal("499.99"));
        Price recurring = Price.recurring(12, RecurrenceUnit.MONTHS, new BigDecimal("29.99"));

        assertThatCode(() -> validator.validatePrice(oneTime)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePrice(recurring)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateItemAttributes("tv-1", ItemAction.ADD, List.of(oneTime, recurring)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidPricesAndMissingItemAttributes() {
        Price validPrice = Price.oneTime(new BigDecimal("99.99"));

        assertThatThrownBy(() -> validator.validatePrice(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(Price.oneTime(null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(Price.oneTime(BigDecimal.ZERO)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(Price.oneTime(new BigDecimal("-5.00"))))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(new Price(PriceType.ONE_TIME, BigDecimal.TEN, 6, null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(new Price(PriceType.ONE_TIME, BigDecimal.TEN, null, RecurrenceUnit.MONTHS)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(Price.recurring(0, RecurrenceUnit.MONTHS, BigDecimal.TEN)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(Price.recurring(12, null, BigDecimal.TEN)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validatePrice(new Price(PriceType.RECURRING, BigDecimal.TEN, null, RecurrenceUnit.DAYS)))
                .isInstanceOf(DomainValidationException.class);

        String oversizedId = "a".repeat(CartValidator.MAX_IDENTIFIER_LENGTH + 1);
        assertThatThrownBy(() -> validator.validateCustomerId(oversizedId))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes(oversizedId, ItemAction.ADD, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes("  ", ItemAction.ADD, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes("tv-1", null, List.of(validPrice)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes("tv-1", ItemAction.ADD, List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes("tv-1", ItemAction.ADD, Collections.singletonList(null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validateItemAttributes(
                "tv-1",
                ItemAction.ADD,
                Collections.nCopies(CartValidator.MAX_PRICES_PER_ITEM + 1, validPrice)
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldValidateCartLifecycleAndItemRemoval() {
        assertThatThrownBy(() -> validator.validateCustomerId(" "))
                .isInstanceOf(DomainValidationException.class);

        ShoppingCart cart = CartTestFixtures.emptyCart("cust-101");
        assertThatThrownBy(() -> validator.validateCheckout(cart))
                .isInstanceOf(InvalidCartStateException.class);

        CartItem firstItem = CartTestFixtures.item("tv-55", ItemAction.ADD, Instant.now());
        CartItem secondItem = CartTestFixtures.item("fiber-1", ItemAction.MODIFY, Instant.now());
        cart.getItems().add(firstItem);
        cart.getItems().add(secondItem);

        assertThatCode(() -> validator.validateItemRemoval(cart, firstItem.itemId())).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateItemRemoval(cart, "unknown-id"))
                .isInstanceOf(ResourceNotFoundException.class);

        validator.validateCheckout(cart);

        cart.setStatus(CartStatus.CHECKED_OUT);
        assertThatThrownBy(() -> validator.validateActiveStatus(cart))
                .isInstanceOf(InvalidCartStateException.class);
    }
}