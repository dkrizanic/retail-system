package com.retailsystem.cart.integration;

import static com.retailsystem.cart.util.CartTestFixtures.cartWithItems;
import static com.retailsystem.cart.util.CartTestFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class OfferStatisticsIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldAggregateOfferStatisticsAndExcludeConvertedCustomersFromRetargeting() {
        Instant windowStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant windowEnd = Instant.parse("2026-06-30T23:59:59Z");
        Instant insideWindow1 = Instant.parse("2026-06-10T10:00:00Z");
        Instant insideWindow2 = Instant.parse("2026-06-15T14:30:00Z");
        Instant beforeWindow = Instant.parse("2026-05-20T09:00:00Z");
        Instant afterWindow = Instant.parse("2026-07-05T12:00:00Z");

        shoppingCartRepository.saveAll(List.of(
                cartWithItems("cust-1", CartStatus.EVICTED, List.of(item("tv-55", ItemAction.ADD, insideWindow1)), insideWindow1),
                cartWithItems("cust-1", CartStatus.CHECKED_OUT, List.of(item("tv-55", ItemAction.ADD, insideWindow2)), insideWindow2),
                cartWithItems("cust-2", CartStatus.ACTIVE, List.of(
                        item("tv-55", ItemAction.ADD, insideWindow1),
                        item("tv-55", ItemAction.ADD, insideWindow2),
                        item("tv-55", ItemAction.MODIFY, insideWindow1),
                        item("tv-55", ItemAction.ADD, beforeWindow),
                        item("soundbar-1", ItemAction.ADD, insideWindow1)
                ), insideWindow1),
                cartWithItems("cust-3", CartStatus.EVICTED, List.of(item("tv-55", ItemAction.ADD, insideWindow2)), insideWindow2),
                cartWithItems("cust-4", CartStatus.CHECKED_OUT, List.of(item("tv-55", ItemAction.ADD, insideWindow1)), insideWindow1),
                cartWithItems("cust-5", CartStatus.ACTIVE, List.of(item("tv-55", ItemAction.MODIFY, insideWindow1)), insideWindow1),
                cartWithItems("cust-6", CartStatus.EVICTED, List.of(item("tv-55", ItemAction.ADD, beforeWindow)), beforeWindow),
                cartWithItems("cust-7", CartStatus.ACTIVE, List.of(item("tv-55", ItemAction.ADD, afterWindow)), afterWindow)
        ));

        ResponseEntity<OfferStatisticsResponse> response = queryValidStatistics(
                "tv-55", "ADD", windowStart.toString(), windowEnd.toString()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OfferStatisticsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.offerId()).isEqualTo("tv-55");
        assertThat(body.action()).isEqualTo(ItemAction.ADD);
        assertThat(body.soldCount()).isEqualTo(2L);
        assertThat(body.activeCartCount()).isEqualTo(2L);
        assertThat(body.evictedCartCount()).isEqualTo(2L);
        assertThat(body.totalAddedCount()).isEqualTo(6L);
        assertThat(body.retargetingCustomerIds())
                .containsExactly("cust-2", "cust-3")
                .doesNotContain("cust-1", "cust-4", "cust-5", "cust-6", "cust-7");

        ResponseEntity<OfferStatisticsResponse> limitedResponse = queryStatistics(
                "tv-55", "ADD", windowStart.toString(), windowEnd.toString(), 1, OfferStatisticsResponse.class
        );
        assertThat(limitedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(limitedResponse.getBody()).isNotNull();
        assertThat(limitedResponse.getBody().totalAddedCount()).isEqualTo(6L);
        assertThat(limitedResponse.getBody().retargetingCustomerIds()).containsExactly("cust-2");
    }

    @Test
    void shouldReturnZeroCountsAndEmptyRetargetingListWhenNoOffersMatch() {
        ResponseEntity<OfferStatisticsResponse> response = queryValidStatistics(
                "non-existent-offer", "DELETE", "2026-06-01T00:00:00Z", "2026-06-30T23:59:59Z"
        );

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
        String earlier = "2026-06-01T00:00:00Z";
        String later = "2026-06-30T00:00:00Z";

        assertThat(queryStatistics("tv-55", "ADD", later, earlier, ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(queryStatistics("   ", "ADD", earlier, later, ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(queryStatistics("tv-55", "UNKNOWN_ACTION", earlier, later, ProblemDetail.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<ProblemDetail> missingParamsResponse = restClient.get()
                .uri("/api/v1/statistics/offers?offerId=tv-55")
                .retrieve()
                .toEntity(ProblemDetail.class);
        assertThat(missingParamsResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
