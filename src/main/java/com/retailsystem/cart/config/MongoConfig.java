package com.retailsystem.cart.config;

import com.retailsystem.cart.entity.ShoppingCart;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new BigDecimalToDecimal128Converter(),
                new Decimal128ToBigDecimalConverter()
        ));
    }

    @Bean
    public SmartInitializingSingleton shoppingCartIndexInitializer(MongoTemplate mongoTemplate) {
        return () -> {
            IndexOperations indexOps = mongoTemplate.indexOps(ShoppingCart.class);

            Index activeCartPartialUniqueIndex = new Index()
                    .on("customerId", Sort.Direction.ASC)
                    .named("idx_active_customer_cart")
                    .unique()
                    .partial(PartialIndexFilter.of(new Document("status", "ACTIVE")));
            indexOps.createIndex(activeCartPartialUniqueIndex);

            Document compoundKeys = new Document()
                    .append("items.offerId", 1)
                    .append("items.action", 1)
                    .append("items.addedAt", 1)
                    .append("status", 1);
            CompoundIndexDefinition analyticsCompoundIndex = (CompoundIndexDefinition) new CompoundIndexDefinition(compoundKeys)
                    .named("idx_offer_statistics");
            indexOps.createIndex(analyticsCompoundIndex);
        };
    }

    @WritingConverter
    static class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {

        @Override
        public Decimal128 convert(BigDecimal source) {
            return new Decimal128(source.round(MathContext.DECIMAL128));
        }
    }

    @ReadingConverter
    static class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {

        @Override
        public BigDecimal convert(Decimal128 source) {
            return source.bigDecimalValue();
        }
    }
}
