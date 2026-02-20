package com.revpay.controller;

import com.revpay.model.dto.*;
import com.revpay.model.entity.Loan;
import com.revpay.model.entity.LoanInstallment;
import com.revpay.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public LoanResponseDTO applyLoan(
            @RequestBody LoanApplyDTO dto,
            Authentication auth){
        return loanService.applyLoan(Long.valueOf(auth.getName()), dto);
    }

    @PostMapping("/repay")
    public String repayLoan(
            @RequestBody LoanRepayDTO dto,
            Authentication auth){
        return loanService.repayLoan(Long.valueOf(auth.getName()), dto);
    }

    @GetMapping("/my")
    public List<Loan> myLoans(Authentication auth){
        return loanService.getUserLoans(Long.valueOf(auth.getName()));
    }

    @GetMapping("/outstanding")
    public BigDecimal totalOutstanding(Authentication auth){
        return loanService.totalOutstanding(Long.valueOf(auth.getName()));
    }

    @GetMapping("/emi/{loanId}")
    public List<LoanInstallment> viewEmiSchedule(
            @PathVariable Long loanId,
            Authentication auth){
        return loanService.getEmiSchedule(Long.valueOf(auth.getName()), loanId);
    }

    @GetMapping("/overdue")
    public List<LoanInstallment> getOverdues(Authentication auth){
        return loanService.getOverdueEmis(Long.valueOf(auth.getName()));
    }

    @GetMapping("/analytics")
    public LoanAnalyticsDTO getAnalytics(Authentication auth){
        Long userId = Long.valueOf(auth.getName());
        return LoanAnalyticsDTO.builder()
                .totalOutstanding(loanService.totalOutstanding(userId))
                .totalPaid(loanService.totalPaid(userId))
                .totalPending(loanService.totalPending(userId))
                .build();
    }

    @PostMapping("/preclose/{loanId}")
    public String preCloseLoan(
            @PathVariable Long loanId,
            Authentication auth){
        return loanService.preCloseLoan(Long.valueOf(auth.getName()), loanId);
    }

    @GetMapping("/credit-score")
    public int getCreditScore(Authentication auth){
        return loanService.calculateCreditScore(Long.valueOf(auth.getName()));
    }

    @GetMapping("/eligibility")
    public LoanEligibilityDTO checkEligibility(Authentication auth){
        return loanService.checkEligibility(Long.valueOf(auth.getName()));
    }

    @GetMapping("/recommendation")
    public LoanRecommendationDTO getRecommendation(Authentication auth){
        return loanService.getLoanRecommendation(Long.valueOf(auth.getName()));
    }
}