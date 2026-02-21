package com.revpay.service;

import com.revpay.model.dto.*;
import com.revpay.model.entity.*;
import com.revpay.repository.LoanInstallmentRepository;
import com.revpay.repository.LoanRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.EmiCalculator;
import com.revpay.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final LoanInstallmentRepository installmentRepository;
    private final NotificationService notificationService;

    public LoanResponseDTO applyLoan(Long userId, LoanApplyDTO dto) {
        log.info("Applying loan for userId: {}, amount: {}", userId, dto.getAmount());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getRole().name().equals("BUSINESS")) {
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
        log.debug("Checking eligibility for userId: {}", userId);

        if (!isEligibleForLoan(userId, dto.getAmount())) {
            throw new RuntimeException("Loan amount exceeds eligibility");
        }

        loanRepository.save(loan);
        log.info("Loan application saved with id: {}", loan.getLoanId());

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

    public String approveLoan(LoanApprovalDTO dto) {

        log.info("Loan approval process started for loanId: {}", dto.getLoanId());

        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> {
                    log.error("Loan not found for loanId: {}", dto.getLoanId());
                    return new RuntimeException("Loan not found");
                });

        if (dto.isApproved()) {

            log.info("Loan approved for loanId: {}", dto.getLoanId());

            BigDecimal interest;

            if (dto.getInterestRate() != null) {
                interest = dto.getInterestRate();
                log.debug("Manual interest rate provided: {}", interest);
            } else {
                interest = getDynamicInterest(loan.getUser().getUserId());
                log.debug("Dynamic interest calculated: {}", interest);
            }

            loan.setInterestRate(interest);
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setStartDate(LocalDate.now());
            loan.setEndDate(LocalDate.now().plusMonths(loan.getTenureMonths()));

            log.debug("Loan activated. StartDate: {}, EndDate: {}",
                    loan.getStartDate(), loan.getEndDate());

            BigDecimal emi = EmiCalculator.calculateEMI(
                    loan.getAmount(),
                    interest,
                    loan.getTenureMonths()
            );

            loan.setEmiAmount(emi);

            log.info("EMI calculated for loanId {}: {}", dto.getLoanId(), emi);

            walletService.addFunds(
                    loan.getUser().getUserId(),
                    loan.getAmount(),
                    "Loan Disbursement"
            );

            log.info("Loan amount disbursed to wallet for userId: {}",
                    loan.getUser().getUserId());

            generateInstallments(loan);

            log.info("EMI schedule generated for loanId: {}", dto.getLoanId());

            notificationService.createNotification(
                    loan.getUser().getUserId(),
                    NotificationUtil.loanApproved(),
                    "LOAN"
            );

            log.info("Loan approval notification sent to userId: {}",
                    loan.getUser().getUserId());

        } else {

            log.warn("Loan rejected for loanId: {}", dto.getLoanId());

            loan.setStatus(LoanStatus.REJECTED);

            notificationService.createNotification(
                    loan.getUser().getUserId(),
                    NotificationUtil.loanRejected(),
                    "LOAN"
            );

            log.info("Loan rejection notification sent to userId: {}",
                    loan.getUser().getUserId());
        }

        loanRepository.save(loan);

        log.info("Loan status updated successfully for loanId: {}", dto.getLoanId());

        return "Loan decision processed";
    }

    private void generateInstallments(Loan loan) {

        log.info("Generating EMI installments for loanId: {}", loan.getLoanId());

        for (int i = 1; i <= loan.getTenureMonths(); i++) {

            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .amount(loan.getEmiAmount())
                    .dueDate(LocalDate.now().plusMonths(i))
                    .status(InstallmentStatus.PENDING)
                    .build();

            installmentRepository.save(installment);

            log.debug("Installment {} created with due date {} for loanId: {}",
                    i,
                    installment.getDueDate(),
                    loan.getLoanId());
        }

        log.info("All {} EMI installments generated for loanId: {}",
                loan.getTenureMonths(),
                loan.getLoanId());
    }

    public String repayLoan(Long userId, LoanRepayDTO dto) {

        log.info("Loan repayment initiated for loanId: {} by userId: {}", dto.getLoanId(), userId);

        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> {
                    log.error("Loan not found for loanId: {}", dto.getLoanId());
                    return new RuntimeException("Loan not found");
                });

        if (!loan.getUser().getUserId().equals(userId)) {
            log.warn("Unauthorized repayment attempt by userId: {} for loanId: {}", userId, dto.getLoanId());
            throw new RuntimeException("Unauthorized repayment attempt");
        }

        LoanInstallment nextInstallment =
                installmentRepository.findByLoan_LoanId(loan.getLoanId())
                        .stream()
                        .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                                || i.getStatus() == InstallmentStatus.OVERDUE)
                        .findFirst()
                        .orElseThrow(() -> {
                            log.warn("No pending EMI found for loanId: {}", loan.getLoanId());
                            return new RuntimeException("No pending EMI");
                        });

        BigDecimal payableAmount = nextInstallment.getAmount();

        if (nextInstallment.getStatus() == InstallmentStatus.OVERDUE) {
            payableAmount = payableAmount.add(BigDecimal.valueOf(100));
            log.warn("Overdue EMI detected. Penalty applied for loanId: {}", loan.getLoanId());
        }

        walletService.withdrawFunds(userId, payableAmount);

        log.info("Amount {} deducted from wallet for userId: {}", payableAmount, userId);

        nextInstallment.setStatus(InstallmentStatus.PAID);
        installmentRepository.save(nextInstallment);

        log.info("Installment {} marked as PAID for loanId: {}",
                nextInstallment.getInstallmentNumber(),
                loan.getLoanId());

        loan.setRemainingAmount(
                loan.getRemainingAmount().subtract(nextInstallment.getAmount())
        );

        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            log.info("Loan closed successfully for loanId: {}", loan.getLoanId());
        }

        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getUser().getUserId(),
                NotificationUtil.loanRepayment(nextInstallment.getAmount()),
                "LOAN"
        );

        log.info("Repayment notification sent for loanId: {}", loan.getLoanId());

        return "EMI Paid Successfully";
    }

    public List<Loan> getUserLoans(Long userId) {

        log.info("Fetching loans for userId: {}", userId);

        List<Loan> loans = loanRepository.findByUser_UserId(userId);

        log.debug("Total loans found for userId {}: {}", userId, loans.size());

        return loans;
    }

    public BigDecimal totalOutstanding(Long userId) {

        log.info("Calculating total outstanding amount for userId: {}", userId);

        BigDecimal total = loanRepository.findByUser_UserId(userId)
                .stream()
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total outstanding for userId {}: {}", userId, total);

        return total;
    }

    public List<Loan> getAllLoans() {

        log.info("Fetching all loans from system");

        List<Loan> loans = loanRepository.findAll();

        log.debug("Total loans in system: {}", loans.size());

        return loans;
    }

    public List<LoanInstallment> getEmiSchedule(Long userId, Long loanId) {

        log.info("Fetching EMI schedule for loanId: {} by userId: {}", loanId, userId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> {
                    log.error("Loan not found for loanId: {}", loanId);
                    return new RuntimeException("Loan not found");
                });

        if (!loan.getUser().getUserId().equals(userId)) {
            log.warn("Unauthorized EMI schedule access by userId: {} for loanId: {}", userId, loanId);
            throw new RuntimeException("Unauthorized");
        }

        List<LoanInstallment> installments = installmentRepository.findByLoan_LoanId(loanId);

        log.debug("Total EMI installments fetched for loanId {}: {}", loanId, installments.size());

        return installments;
    }

    public List<LoanInstallment> getOverdueEmis(Long userId) {

        log.info("Fetching overdue EMIs for userId: {}", userId);

        List<LoanInstallment> overdueList = installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PENDING &&
                                i.getDueDate().isBefore(LocalDate.now())
                )
                .toList();

        log.debug("Total overdue EMIs for userId {}: {}", userId, overdueList.size());

        return overdueList;
    }

    public BigDecimal totalPaid(Long userId) {

        log.info("Calculating total paid EMI amount for userId: {}", userId);

        BigDecimal total = installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PAID
                )
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total paid EMI for userId {}: {}", userId, total);

        return total;
    }

    public BigDecimal totalPending(Long userId) {

        log.info("Calculating total pending EMI amount for userId: {}", userId);

        BigDecimal total = installmentRepository.findAll()
                .stream()
                .filter(i ->
                        i.getLoan().getUser().getUserId().equals(userId) &&
                                i.getStatus() == InstallmentStatus.PENDING
                )
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total pending EMI for userId {}: {}", userId, total);

        return total;
    }

    public void markOverdueInstallments() {

        log.info("Starting overdue EMI check process");

        List<LoanInstallment> installments = installmentRepository.findAll();

        log.debug("Total installments fetched: {}", installments.size());

        for (LoanInstallment emi : installments) {

            if (emi.getStatus() == InstallmentStatus.PENDING &&
                    emi.getDueDate().isBefore(LocalDate.now())) {

                log.warn("EMI marked as OVERDUE for loanId: {}, installment: {}",
                        emi.getLoan().getLoanId(),
                        emi.getInstallmentNumber());

                emi.setStatus(InstallmentStatus.OVERDUE);
                installmentRepository.save(emi);

                notificationService.createNotification(
                        emi.getLoan().getUser().getUserId(),
                        "Your EMI is overdue. Please pay immediately.",
                        "LOAN"
                );

                log.info("Overdue notification sent to userId: {}",
                        emi.getLoan().getUser().getUserId());
            }
        }

        log.info("Overdue EMI check process completed");
    }

    public String preCloseLoan(Long userId, Long loanId) {

        log.info("Pre-closure request initiated for loanId: {} by userId: {}", loanId, userId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> {
                    log.error("Loan not found for loanId: {}", loanId);
                    return new RuntimeException("Loan not found");
                });

        if (!loan.getUser().getUserId().equals(userId)) {
            log.warn("Unauthorized pre-closure attempt by userId: {} for loanId: {}", userId, loanId);
            throw new RuntimeException("Unauthorized");
        }

        BigDecimal remaining = loan.getRemainingAmount();
        BigDecimal preClosureCharge = remaining.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalPayable = remaining.add(preClosureCharge);

        log.debug("Remaining: {}, Pre-closure charge: {}, Total payable: {}",
                remaining, preClosureCharge, totalPayable);

        walletService.withdrawFunds(userId, totalPayable);

        log.info("Amount {} deducted for loan pre-closure for userId: {}", totalPayable, userId);

        loan.setRemainingAmount(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.CLOSED);

        loanRepository.save(loan);

        log.info("Loan successfully pre-closed for loanId: {}", loanId);

        notificationService.createNotification(
                loan.getUser().getUserId(),
                "Your loan has been pre-closed successfully.",
                "LOAN"
        );

        log.info("Pre-closure notification sent to userId: {}", loan.getUser().getUserId());

        return "Loan Pre-closed successfully";
    }

    public int calculateCreditScore(Long userId) {

        log.info("Calculating credit score for userId: {}", userId);

        List<LoanInstallment> installments =
                installmentRepository.findAll()
                        .stream()
                        .filter(i -> i.getLoan().getUser().getUserId().equals(userId))
                        .toList();

        log.debug("Total installments found for userId {}: {}", userId, installments.size());

        long paid = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .count();

        long overdue = installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.OVERDUE)
                .count();

        log.debug("Paid installments: {}, Overdue installments: {}", paid, overdue);

        int score = 700 + (int) (paid * 2) - (int) (overdue * 5);

        if (score > 850) score = 850;
        if (score < 300) score = 300;

        log.info("Final credit score for userId {}: {}", userId, score);

        return score;
    }

    private BigDecimal getDynamicInterest(Long userId) {

        log.info("Calculating dynamic interest for userId: {}", userId);

        int score = calculateCreditScore(userId);
        BigDecimal interest;

        if (score >= 750) interest = BigDecimal.valueOf(8);
        else if (score >= 700) interest = BigDecimal.valueOf(10);
        else if (score >= 650) interest = BigDecimal.valueOf(12);
        else interest = BigDecimal.valueOf(15);

        log.debug("Base interest based on score {}: {}", score, interest);

        BigDecimal finalInterest = applyVipDiscount(userId, interest);

        log.info("Final interest after VIP adjustment for userId {}: {}", userId, finalInterest);

        return finalInterest;
    }

    public RiskTier getRiskTier(Long userId) {

        log.info("Evaluating risk tier for userId: {}", userId);

        int score = calculateCreditScore(userId);
        RiskTier tier;

        if (score >= 750) tier = RiskTier.LOW;
        else if (score >= 650) tier = RiskTier.MEDIUM;
        else tier = RiskTier.HIGH;

        log.debug("Risk tier for userId {} with score {}: {}", userId, score, tier);

        return tier;
    }

    public BigDecimal getLoanLimit(Long userId) {

        log.info("Calculating loan limit for userId: {}", userId);

        int score = calculateCreditScore(userId);
        BigDecimal limit;

        if (score >= 750) limit = BigDecimal.valueOf(1000000);
        else if (score >= 700) limit = BigDecimal.valueOf(500000);
        else if (score >= 650) limit = BigDecimal.valueOf(200000);
        else limit = BigDecimal.valueOf(50000);

        log.debug("Loan limit for userId {} with score {}: {}", userId, score, limit);

        return limit;
    }

    public boolean isEligibleForLoan(Long userId, BigDecimal requestedAmount) {

        log.info("Checking loan eligibility for userId: {} with requested amount: {}", userId, requestedAmount);

        BigDecimal limit = getLoanLimit(userId);
        RiskTier risk = getRiskTier(userId);

        if (risk == RiskTier.HIGH) {
            log.warn("UserId {} is HIGH risk. Loan not eligible", userId);
            return false;
        }

        boolean eligible = requestedAmount.compareTo(limit) <= 0;

        log.debug("Eligibility check for userId {}: Requested {}, Limit {}, Eligible: {}",
                userId, requestedAmount, limit, eligible);

        return eligible;
    }

    public LoanEligibilityDTO checkEligibility(Long userId) {

        log.info("Fetching loan eligibility details for userId: {}", userId);

        int score = calculateCreditScore(userId);
        RiskTier risk = getRiskTier(userId);
        BigDecimal limit = getLoanLimit(userId);

        boolean eligible = risk != RiskTier.HIGH;

        log.debug("Eligibility result -> Score: {}, Risk: {}, Limit: {}, Eligible: {}",
                score, risk, limit, eligible);

        return LoanEligibilityDTO.builder()
                .creditScore(score)
                .riskTier(risk)
                .maxEligibleAmount(limit)
                .eligible(eligible)
                .build();
    }

    public VipTier getVipTier(Long userId) {

        log.info("Evaluating VIP tier for userId: {}", userId);

        int score = calculateCreditScore(userId);
        VipTier tier;

        if (score >= 780) tier = VipTier.PLATINUM;
        else if (score >= 720) tier = VipTier.GOLD;
        else tier = VipTier.NONE;

        log.debug("VIP tier for userId {} with score {}: {}", userId, score, tier);

        return tier;
    }

    private BigDecimal applyVipDiscount(Long userId, BigDecimal baseInterest) {

        log.info("Applying VIP discount for userId: {}", userId);

        VipTier vip = getVipTier(userId);
        BigDecimal finalInterest;

        if (vip == VipTier.PLATINUM) {
            finalInterest = baseInterest.subtract(BigDecimal.valueOf(2));
        } else if (vip == VipTier.GOLD) {
            finalInterest = baseInterest.subtract(BigDecimal.valueOf(1));
        } else {
            finalInterest = baseInterest;
        }

        log.debug("VIP tier: {}, Base interest: {}, Final interest: {}",
                vip, baseInterest, finalInterest);

        return finalInterest;
    }

    public LoanRecommendationDTO getLoanRecommendation(Long userId) {

        log.info("Generating loan recommendation for userId: {}", userId);

        int score = calculateCreditScore(userId);
        RiskTier risk = getRiskTier(userId);
        VipTier vip = getVipTier(userId);
        BigDecimal limit = getLoanLimit(userId);
        BigDecimal interest = getDynamicInterest(userId);

        BigDecimal recommended;

        if (risk == RiskTier.LOW) {
            recommended = limit;
        } else if (risk == RiskTier.MEDIUM) {
            recommended = limit.multiply(BigDecimal.valueOf(0.7));
        } else {
            recommended = limit.multiply(BigDecimal.valueOf(0.4));
        }

        log.debug("Recommendation -> Score: {}, Risk: {}, VIP: {}, Limit: {}, Recommended: {}, Interest: {}",
                score, risk, vip, limit, recommended, interest);

        return LoanRecommendationDTO.builder()
                .creditScore(score)
                .riskTier(risk)
                .vipTier(vip)
                .recommendedAmount(recommended)
                .expectedInterest(interest)
                .build();
    }
}