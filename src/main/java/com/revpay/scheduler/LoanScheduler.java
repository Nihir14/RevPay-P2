package com.revpay.scheduler;

import com.revpay.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanScheduler {

    private final LoanService loanService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void runOverdueCheck(){
        loanService.markOverdueInstallments();
    }
}