package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.OneTimePrice;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.RecurringPrice;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.RecurrenceUnit;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.InvalidCartStateException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShoppingCartTest {

    @Test
    void shouldRejectBlankCustomerId() {
        assertThatThrownBy(() -> ShoppingCart.createNew(null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> ShoppingCart.createNew("   "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void shouldAddAndRemoveItemsInActiveCart() {
        ShoppingCart cart = ShoppingCart.createNew("cust-marko-101");
        List<Price> tvPrice = List.of(new OneTimePrice(new BigDecimal("599.99")));
        List<Price> fiberPrices = List.of(
                new OneTimePrice(new BigDecimal("19.99")),
                new RecurringPrice(24, RecurrenceUnit.MONTHS, new BigDecimal("34.99"))
        );

        CartItem firstItem = cart.addItem("tv-philips-55", ItemAction.ADD, tvPrice);
        CartItem secondItem = cart.addItem("fiber-1gbps", ItemAction.MODIFY, fiberPrices);

        assertThat(cart.getId()).isNotBlank();
        assertThat(cart.getCustomerId()).isEqualTo("cust-marko-101");
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getItems()).containsExactly(firstItem, secondItem);
        assertThat(cart.getUpdatedAt()).isAfterOrEqualTo(cart.getCreatedAt());

        cart.removeItem(firstItem.itemId());

        assertThat(cart.getItems()).containsExactly(secondItem);
    }

    @Test
    void shouldThrowResourceNotFoundWhenRemovingUnknownItem() {
        ShoppingCart cart = ShoppingCart.createNew("cust-marko-101");
        cart.addItem("tv-philips-55", ItemAction.ADD, List.of(new OneTimePrice(new BigDecimal("599.99"))));

        assertThatThrownBy(() -> cart.removeItem("unknown-item-id"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> cart.removeItem(" "))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldTransitionActiveCartToEvictedAndRejectSubsequentMutations() {
        ShoppingCart cart = ShoppingCart.createNew("cust-ana-202");
        CartItem item = cart.addItem(
                "stream-max",
                ItemAction.ADD,
                List.of(new RecurringPrice(12, RecurrenceUnit.MONTHS, new BigDecimal("14.99")))
        );

        cart.evict();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.EVICTED);
        assertThat(cart.getCompletedAt()).isNotNull();
        assertThat(cart.getItems()).containsExactly(item);

        assertThatThrownBy(() -> cart.addItem("tv-1", ItemAction.ADD, List.of(new OneTimePrice(BigDecimal.TEN))))
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(() -> cart.removeItem(item.itemId()))
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(cart::evict)
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(cart::checkout)
                .isInstanceOf(InvalidCartStateException.class);
    }

    @Test
    void shouldCheckoutNonEmptyActiveCartAndRejectSubsequentMutations() {
        ShoppingCart cart = ShoppingCart.createNew("cust-marko-101");
        CartItem item = cart.addItem(
                "tv-philips-55",
                ItemAction.ADD,
                List.of(new OneTimePrice(new BigDecimal("599.99")))
        );

        cart.checkout();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(cart.getCompletedAt()).isNotNull();

        assertThatThrownBy(() -> cart.addItem("tv-1", ItemAction.ADD, List.of(new OneTimePrice(BigDecimal.TEN))))
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(() -> cart.removeItem(item.itemId()))
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(cart::evict)
                .isInstanceOf(InvalidCartStateException.class);
        assertThatThrownBy(cart::checkout)
                .isInstanceOf(InvalidCartStateException.class);
    }

    @Test
    void shouldRejectCheckoutWhenActiveCartIsEmpty() {
        ShoppingCart cart = ShoppingCart.createNew("cust-empty-303");

        assertThatThrownBy(cart::checkout)
                .isInstanceOf(InvalidCartStateException.class);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getCompletedAt()).isNull();
    }
}
