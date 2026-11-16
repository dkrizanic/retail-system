package com.retailsystem.cart.service;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.enums.ItemAction;
import java.time.Instant;

public interface StatisticsService {

    OfferStatisticsResponse getOfferStatistics(String offerId, ItemAction action, Instant from, Instant to);
}
