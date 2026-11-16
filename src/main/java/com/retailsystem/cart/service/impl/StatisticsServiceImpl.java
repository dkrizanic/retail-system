package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.write.OfferStatisticsQuery;
import com.retailsystem.cart.mapper.CartMapper;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository.OfferStatisticsResult;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.StatisticsService;
import com.retailsystem.cart.validation.StatisticsValidator;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final StatisticsValidator statisticsValidator;
    private final CartMapper cartMapper;

    public StatisticsServiceImpl(
            ShoppingCartRepository shoppingCartRepository,
            StatisticsValidator statisticsValidator,
            CartMapper cartMapper
    ) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.statisticsValidator = statisticsValidator;
        this.cartMapper = cartMapper;
    }

    @Override
    public OfferStatisticsResponse getOfferStatistics(OfferStatisticsQuery query) {
        statisticsValidator.validateTimeRange(query.from(), query.to());
        String normalizedOfferId = query.offerId().trim();
        OfferStatisticsResult result = shoppingCartRepository.aggregateOfferStatistics(
                normalizedOfferId,
                query.action(),
                query.from(),
                query.to(),
                query.retargetingLimit()
        );
        return cartMapper.toStatisticsResponse(normalizedOfferId, query.action(), query.from(), query.to(), result);
    }
}
