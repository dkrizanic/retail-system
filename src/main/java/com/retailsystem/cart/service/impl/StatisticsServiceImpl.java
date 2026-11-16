package com.retailsystem.cart.service.impl;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.exception.DomainValidationException;
import com.retailsystem.cart.repository.CustomOfferStatisticsRepository.OfferStatisticsResult;
import com.retailsystem.cart.repository.ShoppingCartRepository;
import com.retailsystem.cart.service.StatisticsService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final ShoppingCartRepository shoppingCartRepository;

    public StatisticsServiceImpl(ShoppingCartRepository shoppingCartRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
    }

    @Override
    public OfferStatisticsResponse getOfferStatistics(String offerId, ItemAction action, Instant from, Instant to) {
        if (offerId == null || offerId.isBlank()) {
            throw new DomainValidationException("Offer ID must not be blank");
        }
        if (action == null) {
            throw new DomainValidationException("Item action is required");
        }
        if (from == null || to == null) {
            throw new DomainValidationException("Time range parameters 'from' and 'to' are required");
        }
        if (from.isAfter(to)) {
            throw new DomainValidationException("Start timestamp 'from' must not be after 'to'");
        }

        OfferStatisticsResult result = shoppingCartRepository.aggregateOfferStatistics(offerId, action, from, to);
        return OfferStatisticsResponse.from(offerId, action, from, to, result);
    }
}
