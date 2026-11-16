package com.retailsystem.cart.util;

import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.dto.write.OneTimePriceWriteDto;
import com.retailsystem.cart.dto.write.RecurringPriceWriteDto;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.RecurrenceUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CartTestFixtures {

    private CartTestFixtures() {
    }

    public static AddCartItemRequest oneTimeAddRequest(String offerId, String amount) {
        return new AddCartItemRequest(
                offerId,
                ItemAction.ADD,
                List.of(new OneTimePriceWriteDto(new BigDecimal(amount)))
        );
    }

    public static AddCartItemRequest hybridAddRequest(
            String offerId,
            ItemAction action,
            String oneTimeAmount,
            int recurrences,
            RecurrenceUnit unit,
            String recurringAmount
    ) {
        return new AddCartItemRequest(
                offerId,
                action,
                List.of(
                        new OneTimePriceWriteDto(new BigDecimal(oneTimeAmount)),
                        new RecurringPriceWriteDto(recurrences, unit, new BigDecimal(recurringAmount))
                )
        );
    }

    public static CartItem item(String offerId, ItemAction action, Instant addedAt) {
        return new CartItem(
                UUID.randomUUID().toString(),
                offerId,
                action,
                List.of(Price.oneTime(new BigDecimal("199.99"))),
                addedAt
        );
    }

    public static ShoppingCart cartWithItems(
            String customerId,
            CartStatus status,
            List<CartItem> items,
            Instant timestamp
    ) {
        return new ShoppingCart(
                UUID.randomUUID().toString(),
                customerId,
                status,
                items,
                timestamp,
                timestamp,
                status == CartStatus.ACTIVE ? null : timestamp,
                null
        );
    }
}
