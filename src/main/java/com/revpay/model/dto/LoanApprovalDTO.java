package com.revpay.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApprovalDTO {
    private Long loanId;
    private boolean approved;
    private BigDecimal interestRate;
}