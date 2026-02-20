package com.revpay.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanRepayDTO {
    private Long loanId;
    private BigDecimal amount;
}