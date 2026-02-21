package com.revpay.repository;

import com.revpay.model.entity.User;
import com.revpay.model.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // This finds the wallet based on the User's ID property
    Optional<Wallet> findByUserUserId(Long userId);
    Optional<Wallet> findByUser(User user);
}