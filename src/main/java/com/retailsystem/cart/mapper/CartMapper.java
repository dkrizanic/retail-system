package com.retailsystem.cart.mapper;

import com.retailsystem.cart.dto.read.CartItemResponse;
import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.read.PriceReadDto;
import com.retailsystem.cart.dto.read.ShoppingCartResponse;
import com.retailsystem.cart.dto.write.PriceWriteDto;
import com.retailsystem.cart.entity.CartItem;
import com.retailsystem.cart.entity.Price;
import com.retailsystem.cart.entity.ShoppingCart;
import com.retailsystem.cart.enums.ItemAction;
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

    Price toPriceEntity(PriceWriteDto dto);

    List<Price> toPriceEntities(List<PriceWriteDto> dtos);

    PriceReadDto toPriceReadDto(Price price);

    OfferStatisticsResponse toStatisticsResponse(
            String offerId,
            ItemAction action,
            Instant from,
            Instant to,
            OfferStatisticsResult result
    );
}
