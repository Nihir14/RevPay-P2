package com.revpay.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {
    private String receiverIdentifier;
    private BigDecimal amount;
    private String description;
    private String transactionPin;
}