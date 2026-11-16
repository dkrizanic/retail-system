package com.retailsystem.cart.validation;

import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.InvalidCartStateException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CartValidator {

    public void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainValidationException("Customer ID must not be blank");
        }
    }

    public void validateAddItem(String customerId, AddCartItemRequest request) {
        validateCustomerId(customerId);
        if (request == null) {
            throw new DomainValidationException("Add item request is required");
        }
        validateItemAttributes(request.offerId(), request.action(),
                request.prices() == null ? null : request.prices().stream()
                        .map(p -> p == null ? null : p.toEntity())
                        .toList());
    }

    public void validateItemAttributes(String offerId, ItemAction action, List<Price> prices) {
        if (offerId == null || offerId.isBlank()) {
            throw new DomainValidationException("Offer ID must not be blank");
        }
        if (action == null) {
            throw new DomainValidationException("Item action is required");
        }
        if (prices == null || prices.isEmpty() || prices.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("Cart item must contain at least one valid price");
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
        if (price.type() == PriceType.RECURRING) {
            if (price.recurrences() == null || price.recurrences() <= 0) {
                throw new DomainValidationException("Recurrences must be greater than zero");
            }
            if (price.unit() == null) {
                throw new DomainValidationException("Recurrence unit is required");
            }
        }
    }

    public void validateActiveStatus(ShoppingCart cart) {
        if (cart == null || cart.getStatus() != CartStatus.ACTIVE) {
            throw new InvalidCartStateException("Shopping cart is not active");
        }
    }

    public void validateItemRemoval(ShoppingCart cart, String itemId) {
        validateActiveStatus(cart);
        if (itemId == null || itemId.isBlank() || !cart.removeItemById(itemId)) {
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
