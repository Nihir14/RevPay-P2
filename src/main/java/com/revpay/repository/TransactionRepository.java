package com.revpay.repository;

import com.revpay.model.Transaction;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Find all transactions where the user is either the sender or receiver
    List<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);
}