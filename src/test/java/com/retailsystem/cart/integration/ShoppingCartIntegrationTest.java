package com.retailsystem.cart.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailsystem.cart.dto.read.OneTimePriceReadDto;
import com.retailsystem.cart.dto.read.RecurringPriceReadDto;
import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.dto.write.OneTimePriceWriteDto;
import com.retailsystem.cart.dto.write.RecurringPriceWriteDto;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.RecurrenceUnit;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import java.math.BigDecimal;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ShoppingCartIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");

    @LocalServerPort
    private int port;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        shoppingCartRepository.deleteAll();
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @Test
    void shouldPersistDecimal128AndExecuteFullCartLifecycle() {
        String customerId = "cust-lifecycle-1";

        ResponseEntity<ProblemDetail> initialGet = restClient.get()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(initialGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(initialGet.getBody()).isNotNull();
        assertThat(initialGet.getBody().getStatus()).isEqualTo(404);

        AddCartItemRequest firstItemRequest = new AddCartItemRequest(
                "offer-fiber-100",
                ItemAction.ADD,
                List.of(
                        new OneTimePriceWriteDto(new BigDecimal("49.99")),
                        new RecurringPriceWriteDto(12, RecurrenceUnit.MONTHS, new BigDecimal("29.99"))
                )
        );

        ResponseEntity<ShoppingCartResponse> addFirstResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(firstItemRequest)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);

        assertThat(addFirstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ShoppingCartResponse cartAfterFirstAdd = addFirstResponse.getBody();
        assertThat(cartAfterFirstAdd).isNotNull();
        assertThat(cartAfterFirstAdd.customerId()).isEqualTo(customerId);
        assertThat(cartAfterFirstAdd.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cartAfterFirstAdd.items()).hasSize(1);
        assertThat(cartAfterFirstAdd.items().getFirst().prices()).containsExactly(
                new OneTimePriceReadDto(new BigDecimal("49.99")),
                new RecurringPriceReadDto(12, RecurrenceUnit.MONTHS, new BigDecimal("29.99"))
        );

        Document rawCartDoc = mongoTemplate.getCollection("shopping_carts")
                .find(new Document("customerId", customerId))
                .first();
        assertThat(rawCartDoc).isNotNull();
        List<Document> rawItems = rawCartDoc.getList("items", Document.class);
        List<Document> rawPrices = rawItems.getFirst().getList("prices", Document.class);
        assertThat(rawPrices.get(0).get("value")).isInstanceOf(Decimal128.class);
        assertThat(rawPrices.get(1).get("value")).isInstanceOf(Decimal128.class);

        AddCartItemRequest secondItemRequest = new AddCartItemRequest(
                "offer-router-pro",
                ItemAction.MODIFY,
                List.of(new OneTimePriceWriteDto(new BigDecimal("19.99")))
        );

        ResponseEntity<ShoppingCartResponse> addSecondResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secondItemRequest)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
        assertThat(addSecondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addSecondResponse.getBody()).isNotNull();
        assertThat(addSecondResponse.getBody().items()).hasSize(2);

        ResponseEntity<ShoppingCartResponse> getActiveResponse = restClient.get()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
        assertThat(getActiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getActiveResponse.getBody()).isNotNull();
        assertThat(getActiveResponse.getBody().items()).hasSize(2);

        String firstItemId = cartAfterFirstAdd.items().getFirst().itemId();
        ResponseEntity<ShoppingCartResponse> removeResponse = restClient.delete()
                .uri("/api/v1/carts/{customerId}/items/{itemId}", customerId, firstItemId)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removeResponse.getBody()).isNotNull();
        assertThat(removeResponse.getBody().items()).hasSize(1);

        ResponseEntity<ProblemDetail> removeUnknownResponse = restClient.delete()
                .uri("/api/v1/carts/{customerId}/items/{itemId}", customerId, "unknown-item-id")
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(removeUnknownResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ShoppingCartResponse> checkoutResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/checkout", customerId)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkoutResponse.getBody()).isNotNull();
        assertThat(checkoutResponse.getBody().status()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(checkoutResponse.getBody().completedAt()).isNotNull();

        ResponseEntity<ProblemDetail> getAfterCheckout = restClient.get()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(getAfterCheckout.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldEvictActiveCartAndAllowNewActiveCartCreation() {
        String customerId = "cust-evict-1";

        AddCartItemRequest addRequest = new AddCartItemRequest(
                "offer-tv-plus",
                ItemAction.ADD,
                List.of(new OneTimePriceWriteDto(new BigDecimal("10.00")))
        );

        restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addRequest)
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<Void> evictResponse = restClient.delete()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toBodilessEntity();
        assertThat(evictResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ProblemDetail> getAfterEvict = restClient.get()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(getAfterEvict.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        List<ShoppingCart> storedCarts = shoppingCartRepository.findAll();
        assertThat(storedCarts).hasSize(1);
        assertThat(storedCarts.getFirst().getStatus()).isEqualTo(CartStatus.EVICTED);
        assertThat(storedCarts.getFirst().getCompletedAt()).isNotNull();

        ResponseEntity<ShoppingCartResponse> newCartResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addRequest)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
        assertThat(newCartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shoppingCartRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldRejectCheckoutOfEmptyActiveCartWith409Conflict() {
        String customerId = "cust-empty-checkout";
        shoppingCartRepository.save(ShoppingCart.createNew(customerId));

        ResponseEntity<ProblemDetail> checkoutResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/checkout", customerId)
                .retrieve()
                .toEntity(ProblemDetail.class);

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
                  "prices": [
                    { "type": "ONE_TIME", "value": 15.00 }
                  ]
                }
                """;
        ResponseEntity<ProblemDetail> blankOfferResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(blankOfferJson)
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(blankOfferResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String nonPositivePriceJson = """
                {
                  "offerId": "offer-1",
                  "action": "ADD",
                  "prices": [
                    { "type": "RECURRING", "recurrences": 0, "unit": "MONTHS", "value": -5.00 }
                  ]
                }
                """;
        ResponseEntity<ProblemDetail> invalidPriceResponse = restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(nonPositivePriceJson)
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(invalidPriceResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
