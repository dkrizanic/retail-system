package com.retailsystem.cart.integration;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.AddCartItemRequest;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @ServiceConnection
    static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:8.0");

    static {
        MONGO_CONTAINER.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected ShoppingCartRepository shoppingCartRepository;

    @Autowired
    protected MongoTemplate mongoTemplate;

    protected RestClient restClient;

    @BeforeEach
    void setUpBase() {
        shoppingCartRepository.deleteAll();
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    protected <T> ResponseEntity<T> getCart(String customerId, Class<T> responseType) {
        return restClient.get()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toEntity(responseType);
    }

    protected ResponseEntity<ShoppingCartResponse> postItem(String customerId, AddCartItemRequest request) {
        return restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ShoppingCartResponse.class);
    }

    protected <T> ResponseEntity<T> postRawItemJson(String customerId, String rawJson, Class<T> responseType) {
        return restClient.post()
                .uri("/api/v1/carts/{customerId}/items", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rawJson)
                .retrieve()
                .toEntity(responseType);
    }

    protected <T> ResponseEntity<T> deleteItem(String customerId, String itemId, Class<T> responseType) {
        return restClient.delete()
                .uri("/api/v1/carts/{customerId}/items/{itemId}", customerId, itemId)
                .retrieve()
                .toEntity(responseType);
    }

    protected ResponseEntity<Void> evictCart(String customerId) {
        return restClient.delete()
                .uri("/api/v1/carts/{customerId}", customerId)
                .retrieve()
                .toBodilessEntity();
    }

    protected <T> ResponseEntity<T> checkoutCart(String customerId, Class<T> responseType) {
        return restClient.post()
                .uri("/api/v1/carts/{customerId}/checkout", customerId)
                .retrieve()
                .toEntity(responseType);
    }

    protected <T> ResponseEntity<T> queryStatistics(
            String offerId,
            String action,
            String from,
            String to,
            Class<T> responseType
    ) {
        return queryStatistics(offerId, action, from, to, null, responseType);
    }

    protected <T> ResponseEntity<T> queryStatistics(
            String offerId,
            String action,
            String from,
            String to,
            Integer retargetingLimit,
            Class<T> responseType
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/api/v1/statistics/offers")
                            .queryParam("offerId", offerId)
                            .queryParam("action", action)
                            .queryParam("from", from)
                            .queryParam("to", to);
                    if (retargetingLimit != null) {
                        builder.queryParam("retargetingLimit", retargetingLimit);
                    }
                    return builder.build();
                })
                .retrieve()
                .toEntity(responseType);
    }

    protected ResponseEntity<OfferStatisticsResponse> queryValidStatistics(
            String offerId,
            String action,
            String from,
            String to
    ) {
        return queryStatistics(offerId, action, from, to, OfferStatisticsResponse.class);
    }
}
