package com.retailsystem.cart.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.OneTimePrice;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OfferStatisticsIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");

    @LocalServerPort
    private int port;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

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
    void shouldAggregateOfferStatisticsAndExcludeConvertedCustomersFromRetargeting() {
        Instant windowStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-06-30T23:59:59Z");
        Instant insideWindow1 = Instant.parse("2026-06-10T10:00:00Z");
        Instant insideWindow2 = Instant.parse("2026-06-15T14:30:00Z");
        Instant beforeWindow = Instant.parse("2026-05-20T09:00:00Z");
        Instant afterWindow = Instant.parse("2026-07-05T12:00:00Z");

        ShoppingCart cust1Evicted = cartWithItems(
                "cust-1",
                CartStatus.EVICTED,
                List.of(item("tv-55", ItemAction.ADD, insideWindow1)),
                insideWindow1
        );
        ShoppingCart cust1CheckedOut = cartWithItems(
                "cust-1",
                CartStatus.CHECKED_OUT,
                List.of(item("tv-55", ItemAction.ADD, insideWindow2)),
                insideWindow2
        );
        ShoppingCart cust2Active = cartWithItems(
                "cust-2",
                CartStatus.ACTIVE,
                List.of(
                        item("tv-55", ItemAction.ADD, insideWindow1),
                        item("soundbar-1", ItemAction.ADD, insideWindow1)
                ),
                insideWindow1
        );
        ShoppingCart cust3Evicted = cartWithItems(
                "cust-3",
                CartStatus.EVICTED,
                List.of(item("tv-55", ItemAction.ADD, insideWindow2)),
                insideWindow2
        );
        ShoppingCart cust4CheckedOut = cartWithItems(
                "cust-4",
                CartStatus.CHECKED_OUT,
                List.of(item("tv-55", ItemAction.ADD, insideWindow1)),
                insideWindow1
        );
        ShoppingCart cust5ModifyAction = cartWithItems(
                "cust-5",
                CartStatus.ACTIVE,
                List.of(item("tv-55", ItemAction.MODIFY, insideWindow1)),
                insideWindow1
        );
        ShoppingCart cust6BeforeWindow = cartWithItems(
                "cust-6",
                CartStatus.EVICTED,
                List.of(item("tv-55", ItemAction.ADD, beforeWindow)),
                beforeWindow
        );
        ShoppingCart cust7AfterWindow = cartWithItems(
                "cust-7",
                CartStatus.ACTIVE,
                List.of(item("tv-55", ItemAction.ADD, afterWindow)),
                afterWindow
        );

        shoppingCartRepository.saveAll(List.of(
                cust1Evicted,
                cust1CheckedOut,
                cust2Active,
                cust3Evicted,
                cust4CheckedOut,
                cust5ModifyAction,
                cust6BeforeWindow,
                cust7AfterWindow
        ));

        ResponseEntity<OfferStatisticsResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/statistics/offers")
                        .queryParam("offerId", "tv-55")
                        .queryParam("action", "ADD")
                        .queryParam("from", windowStart.toString())
                        .queryParam("to", windowEnd.toString())
                        .build())
                .retrieve()
                .toEntity(OfferStatisticsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OfferStatisticsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offerId()).isEqualTo("tv-55");
        assertThat(body.action()).isEqualTo(ItemAction.ADD);
        assertThat(body.soldCount()).isEqualTo(2L);
        assertThat(body.activeCartCount()).isEqualTo(1L);
        assertThat(body.evictedCartCount()).isEqualTo(2L);
        assertThat(body.totalAddedCount()).isEqualTo(5L);
        assertThat(body.retargetingCustomerIds())
                .containsExactlyInAnyOrder("cust-2", "cust-3")
                .doesNotContain("cust-1", "cust-4", "cust-5", "cust-6", "cust-7");
    }

    @Test
    void shouldReturnZeroCountsAndEmptyRetargetingListWhenNoOffersMatch() {
        Instant windowStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-06-30T23:59:59Z");

        ResponseEntity<OfferStatisticsResponse> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/statistics/offers")
                        .queryParam("offerId", "non-existent-offer")
                        .queryParam("action", "DELETE")
                        .queryParam("from", windowStart.toString())
                        .queryParam("to", windowEnd.toString())
                        .build())
                .retrieve()
                .toEntity(OfferStatisticsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OfferStatisticsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.soldCount()).isZero();
        assertThat(body.activeCartCount()).isZero();
        assertThat(body.evictedCartCount()).isZero();
        assertThat(body.totalAddedCount()).isZero();
        assertThat(body.retargetingCustomerIds()).isEmpty();
    }

    @Test
    void shouldRejectInvalidStatisticsQueryParametersWith400ProblemDetail() {
        Instant from = Instant.parse("2026-06-30T00:00:00Z");
        Instant toBeforeFrom = Instant.parse("2026-06-01T00:00:00Z");

        ResponseEntity<ProblemDetail> invertedWindowResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/statistics/offers")
                        .queryParam("offerId", "tv-55")
                        .queryParam("action", "ADD")
                        .queryParam("from", from.toString())
                        .queryParam("to", toBeforeFrom.toString())
                        .build())
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(invertedWindowResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invertedWindowResponse.getBody()).isNotNull();
        assertThat(invertedWindowResponse.getBody().getStatus()).isEqualTo(400);

        ResponseEntity<ProblemDetail> blankOfferResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/statistics/offers")
                        .queryParam("offerId", "   ")
                        .queryParam("action", "ADD")
                        .queryParam("from", toBeforeFrom.toString())
                        .queryParam("to", from.toString())
                        .build())
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(blankOfferResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<ProblemDetail> invalidActionResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/statistics/offers")
                        .queryParam("offerId", "tv-55")
                        .queryParam("action", "UNKNOWN_ACTION")
                        .queryParam("from", toBeforeFrom.toString())
                        .queryParam("to", from.toString())
                        .build())
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(invalidActionResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<ProblemDetail> missingParamsResponse = restClient.get()
                .uri("/api/v1/statistics/offers?offerId=tv-55")
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(missingParamsResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static CartItem item(String offerId, ItemAction action, Instant addedAt) {
        return new CartItem(
                UUID.randomUUID().toString(),
                offerId,
                action,
                List.of(new OneTimePrice(new BigDecimal("199.99"))),
                addedAt
        );
    }

    private static ShoppingCart cartWithItems(
            String customerId,
            CartStatus status,
            List<CartItem> items,
            Instant timestamp
    ) {
        return new ShoppingCart(
                UUID.randomUUID().toString(),
                customerId,
                status,
                items,
                timestamp,
                timestamp,
                status == CartStatus.ACTIVE ? null : timestamp,
                null
        );
    }
}
