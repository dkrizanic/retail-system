package com.retailsystem.cart.validation;

import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.InvalidCartStateException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;
import org.bson.types.Decimal128;
import org.springframework.stereotype.Component;

@Component
public class CartValidator {

    public static final int MAX_IDENTIFIER_LENGTH = 128;
    public static final int MAX_PRICES_PER_ITEM = 50;
    public static final int MAX_ITEMS_PER_CART = 500;

    public String normalizeCustomerId(String customerId) {
        validateCustomerId(customerId);
        return customerId.trim();
    }

    public void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainValidationException("Customer ID must not be blank");
        }
        if (customerId.trim().length() > MAX_IDENTIFIER_LENGTH) {
            throw new DomainValidationException("Customer ID exceeds maximum length of " + MAX_IDENTIFIER_LENGTH);
        }
    }

    public void validateAddItem(String customerId, String offerId, ItemAction action, List<Price> prices) {
        validateCustomerId(customerId);
        validateItemAttributes(offerId, action, prices);
    }

    public void validateItemAttributes(String offerId, ItemAction action, List<Price> prices) {
        if (offerId == null || offerId.isBlank()) {
            throw new DomainValidationException("Offer ID must not be blank");
        }
        if (offerId.trim().length() > MAX_IDENTIFIER_LENGTH) {
            throw new DomainValidationException("Offer ID exceeds maximum length of " + MAX_IDENTIFIER_LENGTH);
        }
        if (action == null) {
            throw new DomainValidationException("Item action is required");
        }
        if (prices == null || prices.isEmpty() || prices.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("Cart item must contain at least one valid price");
        }
        if (prices.size() > MAX_PRICES_PER_ITEM) {
            throw new DomainValidationException("Cart item cannot exceed " + MAX_PRICES_PER_ITEM + " prices");
        }
        prices.forEach(this::validatePrice);
    }

    public void validatePrice(Price price) {
        if (price == null || price.type() == null) {
            throw new DomainValidationException("Price and price type are required");
        }
        if (price.value() == null || price.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Price value must be greater than zero");
        }
        try {
            new Decimal128(price.value().round(MathContext.DECIMAL128));
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new DomainValidationException("Price value is out of supported decimal range");
        }
        if (price.type() == PriceType.ONE_TIME && (price.recurrences() != null || price.unit() != null)) {
            throw new DomainValidationException("One-time price must not define recurrences or unit");
        }
        if (price.type() == PriceType.RECURRING) {
            if (price.recurrences() == null || price.recurrences() <= 0) {
                throw new DomainValidationException("Recurrences must be greater than zero");
            }
            if (price.unit() == null) {
                throw new DomainValidationException("Recurrence unit is required");
            }
        }
    }

    public void validateCartCapacity(ShoppingCart cart) {
        validateActiveStatus(cart);
        if (cart.getItems().size() >= MAX_ITEMS_PER_CART) {
            throw new InvalidCartStateException("Shopping cart item limit reached (" + MAX_ITEMS_PER_CART + ")");
        }
    }

    public void validateActiveStatus(ShoppingCart cart) {
        if (cart == null || cart.getStatus() != CartStatus.ACTIVE) {
            throw new InvalidCartStateException("Shopping cart is not active");
        }
    }

    public void validateItemRemoval(ShoppingCart cart, String itemId) {
        validateActiveStatus(cart);
        boolean present = itemId != null
                && cart.getItems().stream().anyMatch(item -> item.itemId().equals(itemId.trim()));
        if (!present) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }
    }

    public void validateCheckout(ShoppingCart cart) {
        validateActiveStatus(cart);
        if (cart.getItems().isEmpty()) {
            throw new InvalidCartStateException("Cannot check out an empty shopping cart");
        }
    }
}
