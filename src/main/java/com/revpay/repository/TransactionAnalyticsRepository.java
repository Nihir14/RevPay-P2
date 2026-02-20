package com.revpay.repository;


import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionAnalyticsRepository
        extends JpaRepository<Transaction, Long> {

    // All COMPLETED transactions received by business
    List<Transaction> findByReceiverUserIdAndStatusAndTypeIn(
            Long businessId,
            TransactionStatus status,
            List<TransactionType> types
    );

    // All transactions in date range
    List<Transaction> findByReceiverUserIdAndStatusAndTimestampBetween(
            Long businessId,
            TransactionStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
