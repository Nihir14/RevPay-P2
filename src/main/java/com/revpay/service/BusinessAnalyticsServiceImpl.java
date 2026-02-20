package com.revpay.service;

import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;
import com.revpay.repository.TransactionAnalyticsRepository;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusinessAnalyticsServiceImpl
        implements BusinessAnalyticsService {

    private final TransactionAnalyticsRepository repository;

    public BusinessAnalyticsServiceImpl(
            TransactionAnalyticsRepository repository) {
        this.repository = repository;
    }

    //  Transaction Summary
    @Override
    public BusinessSummaryDTO getTransactionSummary(Long businessId) {

        List<TransactionType> receivedTypes = List.of(
                TransactionType.SEND,
                TransactionType.INVOICE_PAYMENT
        );

        List<Transaction> completed =
                repository.findByReceiverUserIdAndStatusAndTypeIn(
                        businessId,
                        TransactionStatus.COMPLETED,
                        receivedTypes
                );

        BigDecimal totalReceived = completed.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSent = BigDecimal.ZERO; // business send logic later

        BigDecimal pendingAmount = BigDecimal.ZERO; // extend later

        return new BusinessSummaryDTO(
                totalReceived,
                totalSent,
                pendingAmount
        );
    }

    //  Daily Revenue Report
    @Override
    public List<RevenueReportDTO> getDailyRevenue(Long businessId) {

        List<Transaction> transactions =
                repository.findByReceiverUserIdAndStatusAndTypeIn(
                        businessId,
                        TransactionStatus.COMPLETED,
                        List.of(TransactionType.INVOICE_PAYMENT, TransactionType.SEND)
                );

        Map<LocalDate, BigDecimal> grouped =
                transactions.stream()
                        .collect(Collectors.groupingBy(
                                t -> t.getTimestamp().toLocalDate(),
                                Collectors.mapping(
                                        Transaction::getAmount,
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )
                                )
                        ));

        return grouped.entrySet().stream()
                .map(e -> new RevenueReportDTO(
                        e.getKey().toString(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(RevenueReportDTO::period))
                .toList();
    }
}
