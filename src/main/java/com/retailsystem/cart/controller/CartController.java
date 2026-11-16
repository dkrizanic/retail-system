package com.retailsystem.cart.controller;

import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/carts/{customerId}")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ShoppingCartResponse> getActiveCart(@PathVariable String customerId) {
        log.debug("Getting active cart for customer: {}", customerId);
        return ResponseEntity.ok(cartService.getActiveCart(customerId));
    }

    @PostMapping("/items")
    public ResponseEntity<ShoppingCartResponse> addItem(
            @PathVariable String customerId,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        log.info("Adding item {} to cart of customer: {}", request.offerId(), customerId);
        return ResponseEntity.ok(cartService.addItem(customerId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ShoppingCartResponse> removeItem(
            @PathVariable String customerId,
            @PathVariable String itemId
    ) {
        log.info("Removing item {} from cart of customer: {}", itemId, customerId);
        return ResponseEntity.ok(cartService.removeItem(customerId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> evictCart(@PathVariable String customerId) {
        log.info("Evicting cart of customer: {}", customerId);
        cartService.evictCart(customerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<ShoppingCartResponse> checkoutCart(@PathVariable String customerId) {
        log.info("Checking out cart of customer: {}", customerId);
        return ResponseEntity.ok(cartService.checkoutCart(customerId));
    }
}
