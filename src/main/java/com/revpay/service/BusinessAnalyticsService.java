package com.revpay.service;

import com.revpay.dto.DashboardSummaryDTO;
import com.revpay.dto.MonthlyRevenueDTO;
import com.revpay.repository.TransactionAnalyticsRepository;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Month;
import java.util.*;

@Service
public class BusinessAnalyticsService {

    @Autowired
    private TransactionAnalyticsRepository repository;

    // 1️⃣ Dashboard summary
    public DashboardSummaryDTO getDashboardSummary(User businessUser) {

        List<Transaction> transactions =
                repository.findByReceiver(businessUser);

        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalSent = BigDecimal.ZERO;
        long completed = 0;
        long failed = 0;

        for (Transaction tx : transactions) {

            if (tx.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                completed++;

                if (tx.getType() == Transaction.TransactionType.INVOICE_PAYMENT ||
                        tx.getType() == Transaction.TransactionType.ADD_FUNDS) {

                    totalReceived = totalReceived.add(tx.getAmount());
                }

                if (tx.getType() == Transaction.TransactionType.SEND ||
                        tx.getType() == Transaction.TransactionType.WITHDRAW) {

                    totalSent = totalSent.add(tx.getAmount());
                }

            } else if (tx.getStatus() == Transaction.TransactionStatus.FAILED) {
                failed++;
            }
        }

        return new DashboardSummaryDTO(
                totalReceived,
                totalSent,
                completed,
                failed
        );
    }

    // 2️⃣ Monthly revenue
    public List<MonthlyRevenueDTO> getMonthlyRevenue(User businessUser) {

        List<Transaction> transactions =
                repository.findByReceiver(businessUser);

        Map<Month, BigDecimal> revenueMap = new HashMap<>();

        for (Transaction tx : transactions) {

            if (tx.getStatus() == Transaction.TransactionStatus.COMPLETED &&
                    tx.getType() == Transaction.TransactionType.INVOICE_PAYMENT) {

                Month month = tx.getTimestamp().getMonth();

                revenueMap.put(
                        month,
                        revenueMap.getOrDefault(month, BigDecimal.ZERO)
                                .add(tx.getAmount())
                );
            }
        }

        List<MonthlyRevenueDTO> result = new ArrayList<>();

        for (Map.Entry<Month, BigDecimal> entry : revenueMap.entrySet()) {
            result.add(
                    new MonthlyRevenueDTO(
                            entry.getKey().name(),
                            entry.getValue()
                    )
            );
        }

        return result;
    }
}
