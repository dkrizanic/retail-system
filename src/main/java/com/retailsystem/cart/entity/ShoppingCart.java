package com.retailsystem.cart.entity;

import com.retailsystem.cart.enums.CartStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "shopping_carts")
public class ShoppingCart {

    @Id
    private String id;
    private String customerId;
    private CartStatus status;
    private List<CartItem> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    @Version
    private Long version;
}
