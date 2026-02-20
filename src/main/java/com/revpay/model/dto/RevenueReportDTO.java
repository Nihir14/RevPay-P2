package com.revpay.model.dto;
import java.math.BigDecimal;

public record RevenueReportDTO(
        String period,
        BigDecimal amount
) {}
