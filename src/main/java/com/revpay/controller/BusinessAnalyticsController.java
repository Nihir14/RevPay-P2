package com.revpay.controller;

import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;
import com.revpay.service.BusinessAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics/business")
public class BusinessAnalyticsController {

    private final BusinessAnalyticsService analyticsService;

    public BusinessAnalyticsController(
            BusinessAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Transaction Summary
    @GetMapping("/{businessId}/summary")
    public BusinessSummaryDTO getSummary(
            @PathVariable Long businessId) {
        return analyticsService.getTransactionSummary(businessId);
    }

    // Daily Revenue
    @GetMapping("/{businessId}/revenue/daily")
    public List<RevenueReportDTO> getDailyRevenue(
            @PathVariable Long businessId) {
        return analyticsService.getDailyRevenue(businessId);
    }
}
