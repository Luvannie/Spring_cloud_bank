package com.banking.account.service;

import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.common.exception.BankingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Main account service for account lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final BalanceService balanceService;
    private final StatementService statementService;
    private final AccountEventPublisher eventPublisher;
    
    /**
     * Creates a new account for a user.
     */
    @Transactional
    public Account createAccount(CreateAccountRequest request, String createdBy) {
        log.info("Creating account for user: {}", request.getUserId());
        
        Account account = Account.builder()
            .userId(request.getUserId())
            .accountNumber(generateAccountNumber())
            .accountType(request.getAccountType())
            .balance(BigDecimal.ZERO)
            .reservedBalance(BigDecimal.ZERO)
            .status(Account.AccountStatus.ACTIVE)
            .currency(request.getCurrency())
            .dailyTransferLimit(new BigDecimal("100000000"))
            .dailyTransferUsed(BigDecimal.ZERO)
            .build();
        
        Account saved = accountRepository.save(account);
        eventPublisher.publishAccountCreated(saved);
        
        log.info("Created account: {} for user: {}", saved.getId(), request.getUserId());
        return saved;
    }
    
    /**
     * Retrieves account by ID and returns response DTO.
     */
    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found: " + accountId));
    }
    
    /**
     * Gets balance information for an account.
     */
    @Transactional(readOnly = true)
    public Account getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found: " + accountId));
        return account;
    }
    
    /**
     * Freezes an account, preventing all operations.
     */
    @Transactional
    public void freezeAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found: " + accountId));
        
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
        
        eventPublisher.publishAccountStatusChanged(account);
        log.info("Frozen account: {}", accountId);
    }
    
    /**
     * Generates unique account number.
     */
    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }
}