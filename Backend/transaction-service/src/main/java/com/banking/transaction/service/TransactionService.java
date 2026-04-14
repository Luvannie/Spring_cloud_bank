package com.banking.transaction.service;

import com.banking.common.exception.BankingException;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.SagaState;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.repository.SagaStateRepository;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core transaction service for managing transaction lifecycle and state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final SagaStateRepository sagaStateRepository;
    
    /**
     * Create a new pending transaction.
     *
     * @param sagaId the saga identifier
     * @param request the transfer request
     * @return the created transaction
     */
    @Transactional
    public Transaction createPendingTransaction(UUID sagaId, 
            com.banking.transaction.dto.TransferRequest request) {
        
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Transaction already exists for idempotency key: {}", 
                    request.getIdempotencyKey());
                return existing.get();
            }
        }
        
        String referenceNumber = generateReferenceNumber();
        
        Transaction transaction = Transaction.builder()
            .sagaId(sagaId)
            .sourceAccountId(request.getSourceAccountId())
            .targetAccountId(request.getTargetAccountId())
            .targetAccountNumber(request.getTargetAccountNumber())
            .targetBankCode(request.getTargetBankCode())
            .amount(request.getAmount())
            .currency("VND")
            .totalAmount(request.getAmount())
            .status(Transaction.TransactionStatus.PENDING)
            .type(Transaction.TransactionType.TRANSFER)
            .description(request.getDescription())
            .referenceNumber(referenceNumber)
            .idempotencyKey(request.getIdempotencyKey())
            .build();
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Find transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return optional transaction
     */
    public Optional<Transaction> findById(UUID transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * Mark transaction as completed.
     *
     * @param transactionId the transaction ID
     */
    @Transactional
    public void markCompleted(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new BankingException("TRANSACTION_NOT_FOUND", 
                "Transaction not found: " + transactionId));
        
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setProcessedAt(Instant.now());
        transactionRepository.save(transaction);
        
        log.info("Transaction {} marked as completed", transactionId);
    }
    
    /**
     * Mark transaction as failed.
     *
     * @param transactionId the transaction ID
     * @param reason the failure reason
     */
    @Transactional
    public void markFailed(UUID transactionId, String reason) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new BankingException("TRANSACTION_NOT_FOUND", 
                "Transaction not found: " + transactionId));
        
        transaction.setStatus(Transaction.TransactionStatus.FAILED);
        transaction.setProcessedAt(Instant.now());
        transactionRepository.save(transaction);
        
        log.warn("Transaction {} marked as failed: {}", transactionId, reason);
    }
    
    /**
     * Mark transaction as processing.
     *
     * @param transactionId the transaction ID
     */
    @Transactional
    public void markProcessing(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new BankingException("TRANSACTION_NOT_FOUND", 
                "Transaction not found: " + transactionId));
        
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        
        log.info("Transaction {} marked as processing", transactionId);
    }
    
    /**
     * Get transaction response by ID.
     *
     * @param transactionId the transaction ID
     * @return the transaction response DTO
     */
    public TransactionResponse getTransactionResponse(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new BankingException("TRANSACTION_NOT_FOUND", 
                "Transaction not found: " + transactionId));
        
        return TransactionResponse.from(transaction);
    }
    
    /**
     * Find transaction by reference number.
     *
     * @param referenceNumber the reference number
     * @return optional transaction
     */
    public Optional<Transaction> findByReferenceNumber(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber);
    }
    
    /**
     * Generate unique reference number for transactions.
     */
    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}