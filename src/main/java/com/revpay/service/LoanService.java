package com.revpay.service;

import com.revpay.model.dto.*;
import com.revpay.model.entity.*;
import com.revpay.repository.LoanInstallmentRepository;
import com.revpay.repository.LoanRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.EmiCalculator;
import com.revpay.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final LoanInstallmentRepository installmentRepository;
    private final NotificationService notificationService;

    public LoanResponseDTO applyLoan(Long userId, LoanApplyDTO dto){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(!user.getRole().name().equals("BUSINESS")){
            throw new RuntimeException("Only business users can apply for loans");
        }

        Loan loan = Loan.builder()
                .user(user)
                .amount(dto.getAmount())
                .tenureMonths(dto.getTenureMonths())
                .remainingAmount(dto.getAmount())
                .purpose(dto.getPurpose())
                .status(LoanStatus.APPLIED)
                .build();
        if(!isEligibleForLoan(userId, dto.getAmount())){
            throw new RuntimeException("Loan amount exceeds eligibility");
        }

        loanRepository.save(loan);

        notificationService.createNotification(
                user.getUserId(),
                NotificationUtil.loanApplied(dto.getAmount()),
                "LOAN"
        );

        return LoanResponseDTO.builder()
                .loanId(loan.getLoanId())
                .amount(loan.getAmount())
                .emiAmount(BigDecimal.ZERO)
                .remainingAmount(loan.getRemainingAmount())
                .status(loan.getStatus())
                .build();
    }

    public String approveLoan(LoanApprovalDTO dto){

        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if(dto.isApproved()){

            BigDecimal interest;

            if(dto.getInterestRate() != null){
                interest = dto.getInterestRate();
            }else{
                interest = getDynamicInterest(loan.getUser().getUserId());
            }

            loan.setInterestRate(interest);
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setStartDate(LocalDate.now());
            loan.setEndDate(LocalDate.now().plusMonths(loan.getTenureMonths()));

            BigDecimal emi = EmiCalculator.calculateEMI(
                    loan.getAmount(),
                    interest,
                    loan.getTenureMonths()
            );

            loan.setEmiAmount(emi);

            walletService.addFunds(
                    loan.getUser().getUserId(),
                    loan.getAmount(),
                    "Loan Disbursement"
            );

            generateInstallments(loan);

            notificationService.createNotification(
                    loan.getUser().getUserId(),
                    NotificationUtil.loanApproved(),
                    "LOAN"
            );

        } else {
            loan.setStatus(LoanStatus.REJECTED);

            notificationService.createNotification(
                    loan.getUser().getUserId(),
                    NotificationUtil.loanRejected(),
                    "LOAN"
            );
        }

        loanRepository.save(loan);
        return "Loan decision processed";
    }

    private void generateInstallments(Loan loan){

        for(int i=1; i<=loan.getTenureMonths(); i++){

            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .amount(loan.getEmiAmount())
                    .dueDate(LocalDate.now().plusMonths(i))
                    .status(InstallmentStatus.PENDING)
                    .build();

            installmentRepository.save(installment);
        }
    }

    public String repayLoan(Long userId, LoanRepayDTO dto){

        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if(!loan.getUser().getUserId().equals(userId)){
            throw new RuntimeException("Unauthorized repayment attempt");
        }

        LoanInstallment nextInstallment =
                installmentRepository.findByLoan_LoanId(loan.getLoanId())
                        .stream()
                        .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                                || i.getStatus() == InstallmentStatus.OVERDUE)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No pending EMI"));

        BigDecimal payableAmount = nextInstallment.getAmount();

        if(nextInstallment.getStatus() == InstallmentStatus.OVERDUE){
            payableAmount = payableAmount.add(BigDecimal.valueOf(100));
        }

        walletService.withdrawFunds(userId, payableAmount);

        nextInstallment.setStatus(InstallmentStatus.PAID);
        installmentRepository.save(nextInstallment);

        loan.setRemainingAmount(
                loan.getRemainingAmount().subtract(nextInstallment.getAmount())
        );

        if(loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0){
            loan.setStatus(LoanStatus.CLOSED);
        }

        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getUser().getUserId(),
                NotificationUtil.loanRepayment(nextInstallment.getAmount()),
                "LOAN"
        );

        return "EMI Paid Successfully";
    }

    public List<Loan> getUserLoans(Long userId){
        return loanRepository.findByUser_Id(userId);
    }

    public BigDecimal totalOutstanding(Long userId){
        return loanRepository.findByUser_Id(userId)
                .stream()
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Loan> getAllLoans(){
        return loanRepository.findAll();
    }

    public List<LoanInstallment> getEmiSchedule(Long userId, Long loanId){

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if(!loan.getUser().getUserId().equals(userId)){
            throw new RuntimeException("Unauthorized");
        }

        return installmentRepository.findByLoan_LoanId(loanId);
    }

    public List<LoanInstallment> getOverdueEmis(Long userId){

        return installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PENDING &&
                                i.getDueDate().isBefore(LocalDate.now())
                )
                .toList();
    }

    public BigDecimal totalPaid(Long userId){

        return installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PAID
                )
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPending(Long userId){

        return installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PENDING
                )
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markOverdueInstallments(){

        List<LoanInstallment> installments = installmentRepository.findAll();

        for(LoanInstallment emi : installments){

            if(emi.getStatus() == InstallmentStatus.PENDING &&
                    emi.getDueDate().isBefore(LocalDate.now())){

                emi.setStatus(InstallmentStatus.OVERDUE);
                installmentRepository.save(emi);

                notificationService.createNotification(
                        emi.getLoan().getUser().getUserId(),
                        "Your EMI is overdue. Please pay immediately.",
                        "LOAN"
                );
            }
        }
    }

    public String preCloseLoan(Long userId, Long loanId){

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if(!loan.getUser().getUserId().equals(userId)){
            throw new RuntimeException("Unauthorized");
        }

        BigDecimal remaining = loan.getRemainingAmount();
        BigDecimal preClosureCharge = remaining.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalPayable = remaining.add(preClosureCharge);

        walletService.withdrawFunds(userId, totalPayable);

        loan.setRemainingAmount(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.CLOSED);

        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getUser().getUserId(),
                "Your loan has been pre-closed successfully.",
                "LOAN"
        );

        return "Loan Pre-closed successfully";
    }
    public int calculateCreditScore(Long userId){

        List<LoanInstallment> installments =
                installmentRepository.findAll()
                        .stream()
                        .filter(i -> i.getLoan().getUser().getUserId().equals(userId))
                        .toList();

        long paid = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .count();

        long overdue = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.OVERDUE)
                .count();

        int score = 700 + (int)(paid * 2) - (int)(overdue * 5);

        if(score > 850) score = 850;
        if(score < 300) score = 300;

        return score;
    }
    private BigDecimal getDynamicInterest(Long userId){

        int score = calculateCreditScore(userId);
        BigDecimal interest;

        if(score >= 750) interest = BigDecimal.valueOf(8);
        else if(score >= 700) interest = BigDecimal.valueOf(10);
        else if(score >= 650) interest = BigDecimal.valueOf(12);
        else interest = BigDecimal.valueOf(15);

        return applyVipDiscount(userId, interest);
    }
    public RiskTier getRiskTier(Long userId){

        int score = calculateCreditScore(userId);

        if(score >= 750) return RiskTier.LOW;
        if(score >= 650) return RiskTier.MEDIUM;
        return RiskTier.HIGH;
    }
    public BigDecimal getLoanLimit(Long userId){

        int score = calculateCreditScore(userId);

        if(score >= 750) return BigDecimal.valueOf(1000000);
        if(score >= 700) return BigDecimal.valueOf(500000);
        if(score >= 650) return BigDecimal.valueOf(200000);
        return BigDecimal.valueOf(50000);
    }
    public boolean isEligibleForLoan(Long userId, BigDecimal requestedAmount){

        BigDecimal limit = getLoanLimit(userId);
        RiskTier risk = getRiskTier(userId);

        if(risk == RiskTier.HIGH) return false;

        return requestedAmount.compareTo(limit) <= 0;
    }
    public LoanEligibilityDTO checkEligibility(Long userId){

        int score = calculateCreditScore(userId);
        RiskTier risk = getRiskTier(userId);
        BigDecimal limit = getLoanLimit(userId);

        return LoanEligibilityDTO.builder()
                .creditScore(score)
                .riskTier(risk)
                .maxEligibleAmount(limit)
                .eligible(risk != RiskTier.HIGH)
                .build();
    }
    public VipTier getVipTier(Long userId){

        int score = calculateCreditScore(userId);

        if(score >= 780) return VipTier.PLATINUM;
        if(score >= 720) return VipTier.GOLD;
        return VipTier.NONE;
    }
    private BigDecimal applyVipDiscount(Long userId, BigDecimal baseInterest){

        VipTier vip = getVipTier(userId);

        if(vip == VipTier.PLATINUM){
            return baseInterest.subtract(BigDecimal.valueOf(2));
        }

        if(vip == VipTier.GOLD){
            return baseInterest.subtract(BigDecimal.valueOf(1));
        }

        return baseInterest;
    }
    public LoanRecommendationDTO getLoanRecommendation(Long userId){

        int score = calculateCreditScore(userId);
        RiskTier risk = getRiskTier(userId);
        VipTier vip = getVipTier(userId);
        BigDecimal limit = getLoanLimit(userId);
        BigDecimal interest = getDynamicInterest(userId);

        BigDecimal recommended;

        if(risk == RiskTier.LOW){
            recommended = limit;
        }
        else if(risk == RiskTier.MEDIUM){
            recommended = limit.multiply(BigDecimal.valueOf(0.7));
        }
        else{
            recommended = limit.multiply(BigDecimal.valueOf(0.4));
        }

        return LoanRecommendationDTO.builder()
                .creditScore(score)
                .riskTier(risk)
                .vipTier(vip)
                .recommendedAmount(recommended)
                .expectedInterest(interest)
                .build();
    }
}