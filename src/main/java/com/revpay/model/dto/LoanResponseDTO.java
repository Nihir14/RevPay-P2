package com.revpay.model.dto;

import com.revpay.model.entity.LoanStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class LoanResponseDTO {

    private Long loanId;
    private BigDecimal amount;
    private BigDecimal emiAmount;
    private BigDecimal remainingAmount;
    private LoanStatus status;
}