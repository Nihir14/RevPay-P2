package com.revpay.dto;

import java.math.BigDecimal;

public class DashboardSummaryDTO {

    private BigDecimal totalReceived;
    private BigDecimal totalSent;
    private long completedTransactions;
    private long failedTransactions;

    public DashboardSummaryDTO(BigDecimal totalReceived,
                               BigDecimal totalSent,
                               long completedTransactions,
                               long failedTransactions) {
        this.totalReceived = totalReceived;
        this.totalSent = totalSent;
        this.completedTransactions = completedTransactions;
        this.failedTransactions = failedTransactions;
    }

    public BigDecimal getTotalReceived() {
        return totalReceived;
    }

    public BigDecimal getTotalSent() {
        return totalSent;
    }

    public long getCompletedTransactions() {
        return completedTransactions;
    }

    public long getFailedTransactions() {
        return failedTransactions;
    }
}
