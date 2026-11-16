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
import java.util.Objects;
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
    public OfferStatisticsResult aggregateOfferStatistics(
            String offerId,
            ItemAction action,
            Instant from,
            Instant to,
            int retargetingLimit
    ) {
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

        AggregationOperation groupByCustomerStage = context -> new Document("$group", new Document("_id", "$customerId")
                .append("soldCount", sumWhenStatus(CartStatus.CHECKED_OUT))
                .append("activeCartCount", sumWhenStatus(CartStatus.ACTIVE))
                .append("evictedCartCount", sumWhenStatus(CartStatus.EVICTED))
                .append("totalAddedCount", new Document("$sum", 1))
                .append("hasCandidate", new Document("$max", new Document("$cond", List.of(
                        new Document("$in", List.of("$status", List.of(CartStatus.ACTIVE.name(), CartStatus.EVICTED.name()))),
                        1,
                        0
                ))))
                .append("hasPurchased", new Document("$max", new Document("$cond", List.of(
                        new Document("$eq", List.of("$status", CartStatus.CHECKED_OUT.name())),
                        1,
                        0
                ))))
        );

        AggregationOperation facetStage = context -> new Document("$facet", new Document("counts", List.of(
                new Document("$group", new Document("_id", null)
                        .append("soldCount", new Document("$sum", "$soldCount"))
                        .append("activeCartCount", new Document("$sum", "$activeCartCount"))
                        .append("evictedCartCount", new Document("$sum", "$evictedCartCount"))
                        .append("totalAddedCount", new Document("$sum", "$totalAddedCount"))
                )
        )).append("retargeting", List.of(
                new Document("$match", new Document("hasCandidate", 1)
                        .append("hasPurchased", 0)
                        .append("_id", new Document("$ne", null))),
                new Document("$sort", new Document("_id", 1)),
                new Document("$limit", retargetingLimit),
                new Document("$project", new Document("_id", 1))
        )));

        AggregationOptions options = AggregationOptions.builder()
                .maxTime(Duration.ofSeconds(5))
                .allowDiskUse(true)
                .build();

        Aggregation aggregation = Aggregation.newAggregation(
                matchCartsStage,
                unwindItemsStage,
                matchItemsStage,
                groupByCustomerStage,
                facetStage
        ).withOptions(options);

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                ShoppingCart.class,
                Document.class
        );

        Document root = results.getUniqueMappedResult();
        if (root == null) {
            return new OfferStatisticsResult(0L, 0L, 0L, 0L, List.of());
        }

        List<Document> countsList = root.getList("counts", Document.class);
        if (countsList == null || countsList.isEmpty()) {
            return new OfferStatisticsResult(0L, 0L, 0L, 0L, List.of());
        }

        Document counts = countsList.getFirst();
        List<Document> retargetingDocs = root.getList("retargeting", Document.class);
        List<String> retargetingCustomerIds = retargetingDocs == null
                ? List.of()
                : retargetingDocs.stream()
                        .map(doc -> doc.getString("_id"))
                        .filter(Objects::nonNull)
                        .toList();

        return new OfferStatisticsResult(
                toLong(counts.get("soldCount")),
                toLong(counts.get("activeCartCount")),
                toLong(counts.get("evictedCartCount")),
                toLong(counts.get("totalAddedCount")),
                retargetingCustomerIds
        );
    }

    private Document sumWhenStatus(CartStatus status) {
        return new Document("$sum", new Document("$cond", List.of(
                new Document("$eq", List.of("$status", status.name())),
                1,
                0
        )));
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
