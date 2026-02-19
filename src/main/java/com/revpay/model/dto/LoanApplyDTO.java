package com.revpay.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplyDTO {
    private BigDecimal amount;
    private Integer tenureMonths;
    private String purpose;
}