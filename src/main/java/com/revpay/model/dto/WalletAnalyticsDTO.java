package com.revpay.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class WalletAnalyticsDTO {
    private BigDecimal totalSpent;
    private Map<String, BigDecimal> spendingByCategory;
    private Long transactionCount;
}