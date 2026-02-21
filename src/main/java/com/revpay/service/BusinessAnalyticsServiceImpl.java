package com.revpay.service;

import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;
import com.revpay.repository.TransactionAnalyticsRepository;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusinessAnalyticsServiceImpl implements BusinessAnalyticsService {

    private static final Logger log =
            LoggerFactory.getLogger(BusinessAnalyticsServiceImpl.class);

    private final TransactionAnalyticsRepository repository;

    public BusinessAnalyticsServiceImpl(
            TransactionAnalyticsRepository repository) {
        this.repository = repository;
    }


    // Transaction Summary

    @Override
    public BusinessSummaryDTO getTransactionSummary(Long businessId) {

        log.info("Started transaction summary calculation for businessId={}", businessId);

        List<TransactionType> receivedTypes = List.of(
                TransactionType.SEND,
                TransactionType.INVOICE_PAYMENT
        );

        log.debug("Fetching completed transactions for businessId={} with types={}",
                businessId, receivedTypes);

        List<Transaction> completed =
                repository.findByReceiverUserIdAndStatusAndTypeIn(
                        businessId,
                        TransactionStatus.COMPLETED,
                        receivedTypes
                );

        log.debug("Fetched {} completed transactions for businessId={}",
                completed.size(), businessId);

        BigDecimal totalReceived = completed.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSent = BigDecimal.ZERO;   // extend later
        BigDecimal pendingAmount = BigDecimal.ZERO; // extend later

        log.info(
                "Transaction summary calculated for businessId={} | totalReceived={} | totalSent={} | pendingAmount={}",
                businessId, totalReceived, totalSent, pendingAmount
        );

        log.info("Completed transaction summary for businessId={}", businessId);

        return new BusinessSummaryDTO(
                totalReceived,
                totalSent,
                pendingAmount
        );
    }


    // Daily Revenue Report

    @Override
    public List<RevenueReportDTO> getDailyRevenue(Long businessId) {

        log.info("Started daily revenue report generation for businessId={}", businessId);

        List<TransactionType> revenueTypes = List.of(
                TransactionType.INVOICE_PAYMENT,
                TransactionType.SEND
        );

        log.debug("Fetching revenue transactions for businessId={} with types={}",
                businessId, revenueTypes);

        List<Transaction> transactions =
                repository.findByReceiverUserIdAndStatusAndTypeIn(
                        businessId,
                        TransactionStatus.COMPLETED,
                        revenueTypes
                );

        log.debug("Fetched {} transactions for revenue calculation for businessId={}",
                transactions.size(), businessId);

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

        log.debug("Grouped revenue into {} date buckets for businessId={}",
                grouped.size(), businessId);

        List<RevenueReportDTO> report =
                grouped.entrySet().stream()
                        .map(entry -> new RevenueReportDTO(
                                entry.getKey().toString(),
                                entry.getValue()
                        ))
                        .sorted(Comparator.comparing(RevenueReportDTO::period))
                        .toList();

        log.info(
                "Daily revenue report generated successfully for businessId={} with {} records",
                businessId, report.size()
        );

        log.info("Completed daily revenue report generation for businessId={}", businessId);

        return report;
    }
}