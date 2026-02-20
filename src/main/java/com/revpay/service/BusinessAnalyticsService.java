package com.revpay.service;


import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;

import java.util.List;

public interface BusinessAnalyticsService {

    BusinessSummaryDTO getTransactionSummary(Long businessId);

    List<RevenueReportDTO> getDailyRevenue(Long businessId);
}
