package com.revpay.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoicePaymentRequest {
    private Long invoiceId;
    private String transactionPin;
}