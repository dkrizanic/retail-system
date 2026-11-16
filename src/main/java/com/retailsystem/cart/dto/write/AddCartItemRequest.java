package com.retailsystem.cart.dto.write;

import com.retailsystem.cart.enums.ItemAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddCartItemRequest(
        @NotBlank(message = "Offer ID must not be blank")
        String offerId,
        @NotNull(message = "Item action is required")
        ItemAction action,
        @NotEmpty(message = "Cart item must contain at least one price")
        List<@NotNull(message = "Price entry must not be null") @Valid PriceWriteDto> prices
) {
}
