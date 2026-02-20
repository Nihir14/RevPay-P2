package com.revpay.repository;

import com.revpay.model.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // ✅ FIXED: Changed 'Id' to 'UserId' to exactly match the User entity's primary key
    List<Loan> findByUser_UserId(Long userId);
}