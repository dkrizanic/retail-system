package com.retailsystem.cart.repository.impl;

import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.CartStatus;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class CustomOfferStatisticsRepositoryImpl implements CustomOfferStatisticsRepository {

    private final MongoTemplate mongoTemplate;

    public CustomOfferStatisticsRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public OfferStatisticsResult aggregateOfferStatistics(String offerId, ItemAction action, Instant from, Instant to) {
        Date fromDate;
        Date toDate;
        try {
            fromDate = Date.from(from);
            toDate = Date.from(to);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new DomainValidationException("Timestamp out of supported range");
        }

        Criteria elemCriteria = Criteria.where("offerId").is(offerId)
                .and("action").is(action.name())
                .and("addedAt").gte(fromDate).lte(toDate);

        AggregationOperation matchCartsStage = Aggregation.match(Criteria.where("items").elemMatch(elemCriteria));
        AggregationOperation unwindItemsStage = Aggregation.unwind("items");
        AggregationOperation matchItemsStage = Aggregation.match(
                Criteria.where("items.offerId").is(offerId)
                        .and("items.action").is(action.name())
                        .and("items.addedAt").gte(fromDate).lte(toDate)
        );

        AggregationOperation groupStage = context -> new Document("$group", new Document("_id", null)
                .append("soldCount", new Document("$sum", new Document("$cond", List.of(
                        new Document("$eq", List.of("$status", CartStatus.CHECKED_OUT.name())),
                        1,
                        0
                ))))
                .append("activeCartCount", new Document("$sum", new Document("$cond", List.of(
                        new Document("$eq", List.of("$status", CartStatus.ACTIVE.name())),
                        1,
                        0
                ))))
                .append("evictedCartCount", new Document("$sum", new Document("$cond", List.of(
                        new Document("$eq", List.of("$status", CartStatus.EVICTED.name())),
                        1,
                        0
                ))))
                .append("totalAddedCount", new Document("$sum", 1))
                .append("candidateCustomers", new Document("$addToSet", new Document("$cond", List.of(
                        new Document("$in", List.of("$status", List.of(CartStatus.ACTIVE.name(), CartStatus.EVICTED.name()))),
                        "$customerId",
                        "$$REMOVE"
                ))))
                .append("purchasedCustomers", new Document("$addToSet", new Document("$cond", List.of(
                        new Document("$eq", List.of("$status", CartStatus.CHECKED_OUT.name())),
                        "$customerId",
                        "$$REMOVE"
                ))))
        );

        AggregationOperation projectStage = context -> new Document("$project", new Document("_id", 0)
                .append("soldCount", 1)
                .append("activeCartCount", 1)
                .append("evictedCartCount", 1)
                .append("totalAddedCount", 1)
                .append("retargetingCustomerIds", new Document("$setDifference", List.of(
                        "$candidateCustomers",
                        "$purchasedCustomers"
                )))
        );

        AggregationOptions options = AggregationOptions.builder()
                .maxTime(Duration.ofSeconds(5))
                .allowDiskUse(true)
                .build();

        Aggregation aggregation = Aggregation.newAggregation(
                matchCartsStage,
                unwindItemsStage,
                matchItemsStage,
                groupStage,
                projectStage
        ).withOptions(options);

        AggregationResults<OfferStatisticsResult> results = mongoTemplate.aggregate(
                aggregation,
                ShoppingCart.class,
                OfferStatisticsResult.class
        );

        OfferStatisticsResult mappedResult = results.getUniqueMappedResult();
        if (mappedResult == null) {
            return new OfferStatisticsResult(0L, 0L, 0L, 0L, List.of());
        }
        return new OfferStatisticsResult(
                mappedResult.soldCount(),
                mappedResult.activeCartCount(),
                mappedResult.evictedCartCount(),
                mappedResult.totalAddedCount(),
                mappedResult.retargetingCustomerIds().stream().sorted().toList()
        );
    }
}
