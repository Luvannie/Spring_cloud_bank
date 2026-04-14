package com.banking.account.service;

import com.banking.account.entity.Account;
import com.banking.account.entity.Statement;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.StatementRepository;
import com.banking.common.exception.BankingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for statement generation and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {
    
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    
    /**
     * Creates a statement entry for an account transaction.
     */
    @Transactional
    public Statement createStatement(UUID accountId, UUID transactionId,
            Statement.StatementType statementType, BigDecimal amount,
            BigDecimal currentBalance, String description) {
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        BigDecimal balanceBefore = currentBalance;
        BigDecimal balanceAfter;
        
        switch (statementType) {
            case CREDIT -> balanceAfter = currentBalance.add(amount);
            case DEBIT -> balanceAfter = currentBalance.subtract(amount);
            case RESERVE -> balanceAfter = currentBalance;
            case RELEASE -> balanceAfter = currentBalance;
            default -> balanceAfter = currentBalance;
        }
        
        Statement statement = Statement.builder()
            .accountId(accountId)
            .transactionId(transactionId)
            .statementType(statementType)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .description(description)
            .build();
        
        return statementRepository.save(statement);
    }
    
    /**
     * Gets all statements for an account.
     */
    @Transactional(readOnly = true)
    public List<Statement> getStatements(UUID accountId) {
        return statementRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
    
    /**
     * Gets statements for an account with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Statement> getStatements(UUID accountId, Pageable pageable) {
        return statementRepository.findByAccountId(accountId, pageable);
    }
}