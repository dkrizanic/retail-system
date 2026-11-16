package com.retailsystem.cart.controller;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.dto.write.OfferStatisticsQuery;
import com.retailsystem.cart.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/offers")
    public ResponseEntity<OfferStatisticsResponse> getOfferStatistics(@Valid OfferStatisticsQuery query) {
        log.debug("Getting offer statistics: {}", query);
        return ResponseEntity.ok(statisticsService.getOfferStatistics(query));
    }
}
