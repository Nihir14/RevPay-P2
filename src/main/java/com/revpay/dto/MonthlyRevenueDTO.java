package com.revpay.dto;
import java.math.BigDecimal;
public class MonthlyRevenueDTO {

    private String month;
    private BigDecimal amount;

    public MonthlyRevenueDTO(String month, BigDecimal amount) {
        this.month = month;
        this.amount = amount;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
