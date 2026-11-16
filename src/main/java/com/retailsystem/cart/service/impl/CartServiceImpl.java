package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.dto.write.PriceWriteDto;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.CartService;
import com.retailsystem.cart.validation.CartValidator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartValidator cartValidator;

    public CartServiceImpl(ShoppingCartRepository shoppingCartRepository, CartValidator cartValidator) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartValidator = cartValidator;
    }

    @Override
    public ShoppingCartResponse getActiveCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        return ShoppingCartResponse.from(cart);
    }

    @Override
    public ShoppingCartResponse addItem(String customerId, AddCartItemRequest request) {
        cartValidator.validateAddItem(customerId, request);
        ShoppingCart cart = shoppingCartRepository
                .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> ShoppingCart.createNew(customerId));
        List<Price> prices = request.prices().stream()
                .map(PriceWriteDto::toEntity)
                .toList();
        cart.addItem(CartItem.create(request.offerId(), request.action(), prices));
        return ShoppingCartResponse.from(shoppingCartRepository.save(cart));
    }

    @Override
    public ShoppingCartResponse removeItem(String customerId, String itemId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        cartValidator.validateItemRemoval(cart, itemId);
        return ShoppingCartResponse.from(shoppingCartRepository.save(cart));
    }

    @Override
    public void evictCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        cartValidator.validateActiveStatus(cart);
        if (cart.getItems().isEmpty()) {
            shoppingCartRepository.delete(cart);
        } else {
            cart.markEvicted();
            shoppingCartRepository.save(cart);
        }
    }

    @Override
    public ShoppingCartResponse checkoutCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        cartValidator.validateCheckout(cart);
        cart.markCheckedOut();
        return ShoppingCartResponse.from(shoppingCartRepository.save(cart));
    }

    private ShoppingCart findActiveCartOrThrow(String customerId) {
        cartValidator.validateCustomerId(customerId);
        return shoppingCartRepository
                .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active shopping cart not found for customer: " + customerId));
    }
}
