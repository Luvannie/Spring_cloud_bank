package com.banking.payment.repository;

import com.banking.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByPayosOrderCode(String payosOrderCode);
    
    Optional<Payment> findByTransactionId(UUID transactionId);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByExpiresAtBeforeAndStatus(Instant now, Payment.PaymentStatus status);
}
