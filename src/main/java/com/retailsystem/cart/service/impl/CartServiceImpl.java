package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.exception.ResourceNotFoundException;
import com.retailsystem.cart.mapper.CartMapper;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.CartService;
import com.retailsystem.cart.validation.CartValidator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private static final int MAX_CONCURRENCY_RETRIES = 8;

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartValidator cartValidator;
    private final CartMapper cartMapper;

    public CartServiceImpl(
            ShoppingCartRepository shoppingCartRepository,
            CartValidator cartValidator,
            CartMapper cartMapper
    ) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartValidator = cartValidator;
        this.cartMapper = cartMapper;
    }

    @Override
    public ShoppingCartResponse getActiveCart(String customerId) {
        ShoppingCart cart = findActiveCartOrThrow(cartValidator.normalizeCustomerId(customerId));
        return cartMapper.toResponse(cart);
    }

    @Override
    public ShoppingCartResponse addItem(String customerId, AddCartItemRequest request) {
        String normalizedCustomerId = cartValidator.normalizeCustomerId(customerId);
        List<Price> prices = request != null ? cartMapper.toPriceEntities(request.prices()) : null;
        cartValidator.validateAddItem(
                normalizedCustomerId,
                request != null ? request.offerId() : null,
                request != null ? request.action() : null,
                prices
        );
        CartItem newItem = new CartItem(
                UUID.randomUUID().toString(),
                request.offerId().trim(),
                request.action(),
                List.copyOf(prices),
                Instant.now()
        );

        return executeWithConcurrencyRetry(() -> {
            ShoppingCart cart = shoppingCartRepository
                    .findByCustomerIdAndStatus(normalizedCustomerId, CartStatus.ACTIVE)
                    .orElseGet(() -> newActiveCart(normalizedCustomerId));
            cartValidator.validateCartCapacity(cart);
            cart.getItems().add(newItem);
            cart.setUpdatedAt(Instant.now());
            return cartMapper.toResponse(shoppingCartRepository.save(cart));
        });
    }

    @Override
    public ShoppingCartResponse removeItem(String customerId, String itemId) {
        String normalizedCustomerId = cartValidator.normalizeCustomerId(customerId);
        return executeWithConcurrencyRetry(() -> {
            ShoppingCart cart = findActiveCartOrThrow(normalizedCustomerId);
            cartValidator.validateItemRemoval(cart, itemId);
            cart.getItems().removeIf(item -> item.itemId().equals(itemId.trim()));
            cart.setUpdatedAt(Instant.now());
            return cartMapper.toResponse(shoppingCartRepository.save(cart));
        });
    }

    @Override
    public void evictCart(String customerId) {
        String normalizedCustomerId = cartValidator.normalizeCustomerId(customerId);
        executeWithConcurrencyRetry(() -> {
            ShoppingCart cart = findActiveCartOrThrow(normalizedCustomerId);
            cartValidator.validateActiveStatus(cart);
            if (cart.getItems().isEmpty()) {
                shoppingCartRepository.delete(cart);
            } else {
                completeCart(cart, CartStatus.EVICTED);
                shoppingCartRepository.save(cart);
            }
            return null;
        });
    }

    @Override
    public ShoppingCartResponse checkoutCart(String customerId) {
        String normalizedCustomerId = cartValidator.normalizeCustomerId(customerId);
        return executeWithConcurrencyRetry(() -> {
            ShoppingCart cart = findActiveCartOrThrow(normalizedCustomerId);
            cartValidator.validateCheckout(cart);
            completeCart(cart, CartStatus.CHECKED_OUT);
            return cartMapper.toResponse(shoppingCartRepository.save(cart));
        });
    }

    private ShoppingCart newActiveCart(String normalizedCustomerId) {
        Instant now = Instant.now();
        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID().toString());
        cart.setCustomerId(normalizedCustomerId);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);
        return cart;
    }

    private void completeCart(ShoppingCart cart, CartStatus finalStatus) {
        Instant now = Instant.now();
        cart.setStatus(finalStatus);
        cart.setUpdatedAt(now);
        cart.setCompletedAt(now);
    }

    private ShoppingCart findActiveCartOrThrow(String normalizedCustomerId) {
        return shoppingCartRepository
                .findByCustomerIdAndStatus(normalizedCustomerId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active shopping cart not found for customer: " + normalizedCustomerId));
    }

    private <T> T executeWithConcurrencyRetry(Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return operation.get();
            } catch (DuplicateKeyException | OptimisticLockingFailureException ex) {
                if (attempt >= MAX_CONCURRENCY_RETRIES) {
                    throw ex;
                }
                pauseBrieflyBeforeRetry(attempt);
            }
        }
    }

    private void pauseBrieflyBeforeRetry(int attempt) {
        try {
            long delayMs = ThreadLocalRandom.current().nextLong(3L, 6L + Math.min(attempt, 4) * 4L);
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
