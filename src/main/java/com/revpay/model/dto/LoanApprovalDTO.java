package com.revpay.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApprovalDTO {
    private Long loanId;
    private boolean approved;
    private BigDecimal interestRate;
}