package com.revpay.repository;

import com.revpay.model.Transaction;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAnalyticsRepository
        extends JpaRepository<Transaction, Long> {

    // For business user (receiver)
    List<Transaction> findByReceiver(User receiver);
}
