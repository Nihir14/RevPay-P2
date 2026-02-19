package com.revpay.controller;


import com.revpay.dto.DashboardSummaryDTO;
import com.revpay.dto.MonthlyRevenueDTO;
import com.revpay.service.BusinessAnalyticsService;
import com.revpay.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business/analytics")
public class BusinessAnalyticsController {

    @Autowired
    private BusinessAnalyticsService service;

    // Assume business user is already authenticated
    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary(@RequestAttribute User user) {
        return service.getDashboardSummary(user);
    }

    @GetMapping("/revenue/monthly")
    public List<MonthlyRevenueDTO> getMonthlyRevenue(@RequestAttribute User user) {
        return service.getMonthlyRevenue(user);
    }
}
