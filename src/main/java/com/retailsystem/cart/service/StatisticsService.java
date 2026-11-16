package com.retailsystem.cart.service;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.write.OfferStatisticsQuery;

public interface StatisticsService {

    OfferStatisticsResponse getOfferStatistics(OfferStatisticsQuery query);
}
