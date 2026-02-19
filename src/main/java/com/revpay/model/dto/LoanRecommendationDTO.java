package com.revpay.model.dto;

import com.revpay.model.entity.RiskTier;
import com.revpay.model.entity.VipTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanRecommendationDTO {

    private int creditScore;
    private RiskTier riskTier;
    private VipTier vipTier;
    private BigDecimal recommendedAmount;
    private BigDecimal expectedInterest;
}