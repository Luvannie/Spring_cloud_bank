package com.banking.payment.service;

import com.banking.account.repository.AccountRepository;
import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import com.banking.common.exception.BankingException;
import com.banking.payment.dto.PaymentLinkRequest;
import com.banking.payment.dto.PaymentLinkResponse;
import com.banking.payment.entity.Payment;
import com.banking.payment.exception.PaymentException;
import com.banking.payment.repository.PaymentRepository;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Main payment service for orchestrating payment operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PayOSService payOSService;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    
    /**
     * Creates a payment link for the given request.
     */
    @Transactional
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.info("Creating payment link for transaction: {}", request.getTransactionId());
        
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Amount must be positive");
        }
        
        return payOSService.createPaymentLink(request);
    }

    /**
     * Creates a payment link after verifying the current user owns the transaction.
     */
    @Transactional
    public PaymentLinkResponse createPaymentLinkForUser(PaymentLinkRequest request, String username) {
        verifyTransactionOwnership(request.getTransactionId(), username);
        return createPaymentLink(request);
    }
    
    /**
     * Gets payment by ID.
     */
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId));
    }

    /**
     * Gets payment by ID after verifying the current user owns the underlying transaction.
     */
    public Payment getPaymentForUser(UUID paymentId, String username) {
        Payment payment = getPayment(paymentId);
        verifyTransactionOwnership(payment.getTransactionId(), username);
        return payment;
    }
    
    /**
     * Gets payment by transaction ID.
     */
    public Payment getPaymentByTransactionId(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "Payment not found for transaction: " + transactionId));
    }

    /**
     * Gets payment by transaction ID after verifying the current user owns that transaction.
     */
    public Payment getPaymentByTransactionIdForUser(UUID transactionId, String username) {
        verifyTransactionOwnership(transactionId, username);
        return getPaymentByTransactionId(transactionId);
    }

    private void verifyTransactionOwnership(UUID transactionId, String username) {
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new BankingException("USER_NOT_FOUND", "User not found"));

        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new BankingException("TRANSACTION_NOT_FOUND", "Transaction not found: " + transactionId));

        UUID ownerId = accountRepository.findById(transaction.getSourceAccountId())
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found: " + transaction.getSourceAccountId()))
            .getUserId();

        if (!ownerId.equals(currentUser.getId())) {
            throw new BankingException("ACCESS_DENIED", "You don't have access to this payment");
        }
    }
}
