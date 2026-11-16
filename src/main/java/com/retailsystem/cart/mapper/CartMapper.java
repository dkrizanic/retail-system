package com.retailsystem.cart.mapper;

import com.retailsystem.cart.dto.read.CartItemResponse;
import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.read.OneTimePriceReadDto;
import com.retailsystem.cart.dto.read.PriceReadDto;
import com.retailsystem.cart.dto.read.RecurringPriceReadDto;
import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.OneTimePriceWriteDto;
import com.retailsystem.cart.dto.write.PriceWriteDto;
import com.retailsystem.cart.dto.write.RecurringPriceWriteDto;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.enums.PriceType;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository.OfferStatisticsResult;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "cartId", source = "id")
    ShoppingCartResponse toResponse(ShoppingCart cart);

    CartItemResponse toItemResponse(CartItem item);

    List<Price> toPriceEntities(List<PriceWriteDto> dtos);

    default Price toPriceEntity(PriceWriteDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto) {
            case OneTimePriceWriteDto oneTime -> Price.oneTime(oneTime.value());
            case RecurringPriceWriteDto recurring -> Price.recurring(
                    recurring.recurrences(),
                    recurring.unit(),
                    recurring.value()
            );
        };
    }

    default PriceReadDto toPriceReadDto(Price price) {
        if (price == null) {
            return null;
        }
        if (price.type() == PriceType.RECURRING) {
            return new RecurringPriceReadDto(price.recurrences(), price.unit(), price.value());
        }
        return new OneTimePriceReadDto(price.value());
    }

    @Mapping(target = "offerId", source = "offerId")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "from", source = "from")
    @Mapping(target = "to", source = "to")
    @Mapping(target = "soldCount", source = "result.soldCount")
    @Mapping(target = "activeCartCount", source = "result.activeCartCount")
    @Mapping(target = "evictedCartCount", source = "result.evictedCartCount")
    @Mapping(target = "totalAddedCount", source = "result.totalAddedCount")
    @Mapping(target = "retargetingCustomerIds", source = "result.retargetingCustomerIds")
    OfferStatisticsResponse toStatisticsResponse(
            String offerId,
            ItemAction action,
            Instant from,
            Instant to,
            OfferStatisticsResult result
    );
}
