package com.retailsystem.cart.service;

import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;

public interface CartService {

    ShoppingCartResponse getActiveCart(String customerId);

    ShoppingCartResponse addItem(String customerId, AddCartItemRequest request);

    ShoppingCartResponse removeItem(String customerId, String itemId);

    void evictCart(String customerId);

    ShoppingCartResponse checkoutCart(String customerId);
}
