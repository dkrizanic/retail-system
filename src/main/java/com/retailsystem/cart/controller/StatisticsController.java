package com.retailsystem.cart.controller;

import com.retailsystem.cart.dto.read.OfferStatisticsResponse;
import com.retailsystem.cart.enums.ItemAction;
import com.retailsystem.cart.service.StatisticsService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/offers")
    public ResponseEntity<OfferStatisticsResponse> getOfferStatistics(
            @RequestParam String offerId,
            @RequestParam ItemAction action,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return ResponseEntity.ok(statisticsService.getOfferStatistics(offerId, action, from, to));
    }
}
