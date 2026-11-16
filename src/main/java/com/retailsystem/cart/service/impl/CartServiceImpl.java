package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.dto.write.PriceWriteDto;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.CartService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private final ShoppingCartRepository shoppingCartRepository;

    public CartServiceImpl(ShoppingCartRepository shoppingCartRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
    }

    @Override
    public ShoppingCartResponse getActiveCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        return ShoppingCartResponse.from(cart);
    }

    @Override
    public ShoppingCartResponse addItem(String customerId, AddCartItemRequest request) {
        ShoppingCart cart = shoppingCartRepository
                .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> ShoppingCart.createNew(customerId));
        List<Price> prices = request.prices().stream()
                .map(PriceWriteDto::toEntity)
                .toList();
        cart.addItem(request.offerId(), request.action(), prices);
        ShoppingCart saved = shoppingCartRepository.save(cart);
        return ShoppingCartResponse.from(saved);
    }

    @Override
    public ShoppingCartResponse removeItem(String customerId, String itemId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        cart.removeItem(itemId);
        ShoppingCart saved = shoppingCartRepository.save(cart);
        return ShoppingCartResponse.from(saved);
    }

    @Override
    public void evictCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        if (cart.getItems().isEmpty()) {
            shoppingCartRepository.delete(cart);
        } else {
            cart.evict();
            shoppingCartRepository.save(cart);
        }
    }

    @Override
    public ShoppingCartResponse checkoutCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(customerId);
        cart.checkout();
        ShoppingCart saved = shoppingCartRepository.save(cart);
        return ShoppingCartResponse.from(saved);
    }

    private ShoppingCart findActiveCartOrThrow(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainValidationException("Customer ID must not be blank");
        }
        return shoppingCartRepository
                .findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active shopping cart not found for customer: " + customerId));
    }
}
