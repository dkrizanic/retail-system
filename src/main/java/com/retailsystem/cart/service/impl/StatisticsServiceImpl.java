package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository.OfferStatisticsResult;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.StatisticsService;
import com.retailsystem.cart.validation.StatisticsValidator;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final StatisticsValidator statisticsValidator;

    public StatisticsServiceImpl(
            ShoppingCartRepository shoppingCartRepository,
            StatisticsValidator statisticsValidator
    ) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.statisticsValidator = statisticsValidator;
    }

    @Override
    public OfferStatisticsResponse getOfferStatistics(String offerId, ItemAction action, Instant from, Instant to) {
        statisticsValidator.validateQuery(offerId, action, from, to);
        OfferStatisticsResult result = shoppingCartRepository.aggregateOfferStatistics(offerId, action, from, to);
        return OfferStatisticsResponse.from(offerId, action, from, to, result);
    }
}
