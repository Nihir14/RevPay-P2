package com.revpay.util;

import java.math.BigDecimal;
import java.math.MathContext;

public class EmiCalculator {

    public static BigDecimal calculateEMI(
            BigDecimal principal,
            BigDecimal annualRate,
            int months
    ) {

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12 * 100), MathContext.DECIMAL64);

        BigDecimal onePlusRPowerN =
                (BigDecimal.ONE.add(monthlyRate)).pow(months);

        return principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowerN)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), MathContext.DECIMAL64);
    }
}