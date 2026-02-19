package com.revpay.repository;

import com.revpay.model.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_Id(Long userId);
}