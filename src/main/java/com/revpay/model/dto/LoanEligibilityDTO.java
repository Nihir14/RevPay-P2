package com.revpay.model.dto;

import com.revpay.model.entity.RiskTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanEligibilityDTO {

    private int creditScore;
    private RiskTier riskTier;
    private BigDecimal maxEligibleAmount;
    private boolean eligible;
}