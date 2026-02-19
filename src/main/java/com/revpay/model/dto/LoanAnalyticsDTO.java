package com.revpay.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanAnalyticsDTO {

    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
}