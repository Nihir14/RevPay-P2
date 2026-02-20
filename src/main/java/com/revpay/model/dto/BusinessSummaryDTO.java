package com.revpay.model.dto;


import java.math.BigDecimal;

public record BusinessSummaryDTO(
        BigDecimal totalReceived,
        BigDecimal totalSent,
        BigDecimal pendingAmount
) {}
