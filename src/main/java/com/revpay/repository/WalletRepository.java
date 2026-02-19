package com.revpay.repository;

import com.revpay.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Wallet is linked to User via user_id
}