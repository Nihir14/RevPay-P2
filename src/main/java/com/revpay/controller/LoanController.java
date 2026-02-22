package com.revpay.controller;

import com.revpay.model.dto.*;
import com.revpay.model.entity.Loan;
import com.revpay.model.entity.LoanInstallment;
import com.revpay.repository.UserRepository;
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
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth){
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getUserId();
    }

    @PostMapping("/apply")
    public LoanResponseDTO applyLoan(
            @RequestBody LoanApplyDTO dto,
            Authentication auth){

        return loanService.applyLoan(getUserId(auth), dto);
    }

    @PostMapping("/repay")
    public String repayLoan(
            @RequestBody LoanRepayDTO dto,
            Authentication auth){

        return loanService.repayLoan(getUserId(auth), dto);
    }

    @GetMapping("/my")
    public List<Loan> myLoans(Authentication auth){
        return loanService.getUserLoans(getUserId(auth));
    }

    @GetMapping("/outstanding")
    public BigDecimal totalOutstanding(Authentication auth){
        return loanService.totalOutstanding(Long.valueOf(auth.getName()));
    }

    @GetMapping("/emi/{loanId}")
    public List<LoanInstallment> viewEmiSchedule(
            @PathVariable Long loanId,
            Authentication auth){

        return loanService.getEmiSchedule(getUserId(auth), loanId);
    }

    @GetMapping("/overdue")
    public List<LoanInstallment> getOverdues(Authentication auth){
        return loanService.getOverdueEmis(getUserId(auth));
    }

    @GetMapping("/analytics")
    public LoanAnalyticsDTO getAnalytics(Authentication auth){

        Long userId = getUserId(auth);

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

        return loanService.preCloseLoan(getUserId(auth), loanId);
    }

    @GetMapping("/credit-score")
    public int getCreditScore(Authentication auth){
        return loanService.calculateCreditScore(getUserId(auth));
    }

    @GetMapping("/eligibility")
    public LoanEligibilityDTO checkEligibility(Authentication auth){
        return loanService.checkEligibility(getUserId(auth));
    }

    @GetMapping("/recommendation")
    public LoanRecommendationDTO getRecommendation(Authentication auth){
        return loanService.getLoanRecommendation(getUserId(auth));
    }
}