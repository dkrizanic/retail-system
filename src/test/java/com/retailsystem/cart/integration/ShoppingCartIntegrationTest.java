package com.retailsystem.cart.integration;

import static com.retailsystem.cart.util.CartTestFixtures.hybridAddRequest;
import static com.retailsystem.cart.util.CartTestFixtures.oneTimeAddRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.dto.read.PriceReadDto;
import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.enums.RecurrenceUnit;
import java.math.BigDecimal;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class ShoppingCartIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldPersistDecimal128AndExecuteFullCartLifecycle() {
        String customerId = "cust-lifecycle-1";

        assertThat(getCart(customerId, ProblemDetail.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ShoppingCartResponse> addFirstResponse = postItem(
                customerId,
                hybridAddRequest("offer-fiber-100", ItemAction.ADD, "49.99", 12, RecurrenceUnit.MONTHS, "29.99")
        );

        assertThat(addFirstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ShoppingCartResponse cartAfterFirstAdd = addFirstResponse.getBody();
        assertThat(cartAfterFirstAdd).isNotNull();
        assertThat(cartAfterFirstAdd.customerId()).isEqualTo(customerId);
        assertThat(cartAfterFirstAdd.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cartAfterFirstAdd.items()).hasSize(1);
        assertThat(cartAfterFirstAdd.items().getFirst().prices()).containsExactly(
                new PriceReadDto(PriceType.ONE_TIME, new BigDecimal("49.99"), null, null),
                new PriceReadDto(PriceType.RECURRING, new BigDecimal("29.99"), 12, RecurrenceUnit.MONTHS)
        );

        Document rawCartDoc = mongoTemplate.getCollection("shopping_carts")
                .find(new Document("customerId", customerId))
                .first();
        assertThat(rawCartDoc).isNotNull();
        List<Document> rawPrices = rawCartDoc.getList("items", Document.class)
                .getFirst()
                .getList("prices", Document.class);
        assertThat(rawPrices.get(0).get("value")).isInstanceOf(Decimal128.class);
        assertThat(rawPrices.get(1).get("value")).isInstanceOf(Decimal128.class);

        ResponseEntity<ShoppingCartResponse> addSecondResponse = postItem(
                customerId,
                oneTimeAddRequest("offer-router-pro", "19.99")
        );
        assertThat(addSecondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addSecondResponse.getBody()).isNotNull();
        assertThat(addSecondResponse.getBody().items()).hasSize(2);

        ResponseEntity<ShoppingCartResponse> getActiveResponse = getCart(customerId, ShoppingCartResponse.class);
        assertThat(getActiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getActiveResponse.getBody()).isNotNull();
        assertThat(getActiveResponse.getBody().items()).hasSize(2);

        String firstItemId = cartAfterFirstAdd.items().getFirst().itemId();
        ResponseEntity<ShoppingCartResponse> removeResponse = deleteItem(customerId, firstItemId, ShoppingCartResponse.class);
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removeResponse.getBody()).isNotNull();
        assertThat(removeResponse.getBody().items()).hasSize(1);

        assertThat(deleteItem(customerId, "unknown-item-id", ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ShoppingCartResponse> checkoutResponse = checkoutCart(customerId, ShoppingCartResponse.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkoutResponse.getBody()).isNotNull();
        assertThat(checkoutResponse.getBody().status()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(checkoutResponse.getBody().completedAt()).isNotNull();

        assertThat(getCart(customerId, ProblemDetail.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldEvictActiveCartAndAllowNewActiveCartCreation() {
        String customerId = "cust-evict-1";

        postItem(customerId, oneTimeAddRequest("offer-tv-plus", "10.00"));

        assertThat(evictCart(customerId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getCart(customerId, ProblemDetail.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        List<ShoppingCart> storedCarts = shoppingCartRepository.findAll();
        assertThat(storedCarts).hasSize(1);
        assertThat(storedCarts.getFirst().getStatus()).isEqualTo(CartStatus.EVICTED);
        assertThat(storedCarts.getFirst().getCompletedAt()).isNotNull();

        assertThat(postItem(customerId, oneTimeAddRequest("offer-tv-plus", "10.00")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(shoppingCartRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldRejectCheckoutOfEmptyActiveCartWith409Conflict() {
        String customerId = "cust-empty-checkout";
        shoppingCartRepository.save(ShoppingCart.createNew(customerId));

        ResponseEntity<ProblemDetail> checkoutResponse = checkoutCart(customerId, ProblemDetail.class);

        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(checkoutResponse.getBody()).isNotNull();
        assertThat(checkoutResponse.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void shouldRejectInvalidAddItemPayloadsWith400ProblemDetail() {
        String customerId = "cust-invalid-payload";

        String blankOfferJson = """
                {
                  "offerId": "   ",
                  "action": "ADD",
                  "prices": [{ "type": "ONE_TIME", "value": 15.00 }]
                }
                """;
        assertThat(postRawItemJson(customerId, blankOfferJson, ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        String nonPositivePriceJson = """
                {
                  "offerId": "offer-1",
                  "action": "ADD",
                  "prices": [{ "type": "RECURRING", "recurrences": 0, "unit": "MONTHS", "value": -5.00 }]
                }
                """;
        assertThat(postRawItemJson(customerId, nonPositivePriceJson, ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldEnforcePartialUniqueActiveCartIndexAndAnalyticsIndex() {
        List<IndexInfo> indexes = mongoTemplate.indexOps("shopping_carts").getIndexInfo();
        assertThat(indexes)
                .extracting(IndexInfo::getName)
                .contains("idx_active_customer_cart", "idx_offer_statistics");

        String customerId = "cust-unique-active";
        shoppingCartRepository.save(ShoppingCart.createNew(customerId));

        assertThatThrownBy(() -> shoppingCartRepository.save(ShoppingCart.createNew(customerId)))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
