package com.revpay.repository;

import com.revpay.model.PaymentMethod;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUser(User user);
    // Useful for finding the card marked as default
    List<PaymentMethod> findByUserAndIsDefault(User user, boolean isDefault);
}