package com.revpay.repository;

import com.revpay.model.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanInstallmentRepository
        extends JpaRepository<LoanInstallment, Long> {

    List<LoanInstallment> findByLoan_User_UserId(Long loanId);
}
