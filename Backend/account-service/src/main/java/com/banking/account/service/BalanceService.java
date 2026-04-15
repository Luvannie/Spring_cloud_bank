package com.banking.account.service;

import com.banking.account.entity.Account;
import com.banking.account.entity.BalanceReservation;
import com.banking.account.entity.Statement;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.BalanceReservationRepository;
import com.banking.common.exception.BankingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for balance management with optimistic locking.
 * Handles balance reservations for Saga transactions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private static final BigDecimal DEFAULT_DAILY_TRANSFER_LIMIT = new BigDecimal("100000000");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    
    private final AccountRepository accountRepository;
    private final BalanceReservationRepository reservationRepository;
    private final StatementService statementService;
    
    /**
     * Reserves balance for a transaction.
     * Uses optimistic locking to prevent concurrent modifications.
     */
    @Transactional
    public BalanceReservation reserveBalance(UUID accountId, UUID transactionId, BigDecimal amount) {
        log.info("Reserving balance: {} for account: {}, transaction: {}", amount, accountId, transactionId);
        
        Account account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankingException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }
        
        // Check daily transfer limit
        BigDecimal remainingLimit = getDailyTransferLimit(account)
            .subtract(getDailyTransferUsed(account));
        
        if (amount.compareTo(remainingLimit) > 0) {
            throw new BankingException("DAILY_LIMIT_EXCEEDED",
                String.format("Daily limit exceeded. Remaining: %s, Requested: %s",
                    remainingLimit, amount));
        }
        
        BigDecimal available = account.getAvailableBalance();
        if (available.compareTo(amount) < 0) {
            throw new BankingException("INSUFFICIENT_BALANCE",
                String.format("Insufficient balance. Available: %s, Required: %s", available, amount));
        }
        
        // Reserve balance
        account.setReservedBalance(getReservedBalance(account).add(amount));
        account.setDailyTransferUsed(getDailyTransferUsed(account).add(amount));
        accountRepository.save(account);
        
        // Create reservation record
        BalanceReservation reservation = BalanceReservation.builder()
            .accountId(accountId)
            .transactionId(transactionId)
            .amount(amount)
            .status(BalanceReservation.ReservationStatus.ACTIVE)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
            .build();
        
        BalanceReservation saved = reservationRepository.save(reservation);
        
        // Create statement entry
        statementService.createStatement(accountId, transactionId,
            Statement.StatementType.RESERVE, amount, account.getBalance(), "Balance reserved");
        
        log.info("Reserved balance: {} for account: {}", reservation.getId(), accountId);
        return saved;
    }
    
    /**
     * Commits a reservation, finalizing the balance deduction.
     */
    @Transactional
    public void commitReservation(UUID reservationId) {
        BalanceReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BankingException("RESERVATION_NOT_FOUND", "Reservation not found"));
        
        if (reservation.getStatus() != BalanceReservation.ReservationStatus.ACTIVE) {
            throw new BankingException("RESERVATION_NOT_ACTIVE", "Reservation is not active");
        }
        
        Account account = accountRepository.findByIdWithLock(reservation.getAccountId())
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        // Check account status before committing
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankingException("ACCOUNT_NOT_ACTIVE",
                "Cannot commit on account with status: " + account.getStatus());
        }
        
        // Move from reserved to actual balance deduction
        account.setReservedBalance(getReservedBalance(account).subtract(reservation.getAmount()));
        account.setBalance(account.getBalance().subtract(reservation.getAmount()));
        accountRepository.save(account);
        
        // Update reservation status
        reservation.setStatus(BalanceReservation.ReservationStatus.COMMITTED);
        reservationRepository.save(reservation);
        
        // Create statement
        statementService.createStatement(account.getId(), reservation.getTransactionId(),
            Statement.StatementType.DEBIT, reservation.getAmount(),
            account.getBalance(), "Transfer committed");
        
        log.info("Committed reservation: {} for account: {}", reservationId, account.getId());
    }
    
    /**
     * Rolls back a reservation, releasing the reserved balance.
     */
    @Transactional
    public void rollbackReservation(UUID reservationId) {
        BalanceReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BankingException("RESERVATION_NOT_FOUND", "Reservation not found"));
        
        if (reservation.getStatus() != BalanceReservation.ReservationStatus.ACTIVE) {
            return; // Already rolled back
        }
        
        Account account = accountRepository.findByIdWithLock(reservation.getAccountId())
            .orElseThrow(() -> new BankingException("ACCOUNT_NOT_FOUND", "Account not found"));
        
        // Check account status before rolling back
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            log.warn("Rolling back reservation {} on non-active account: {}",
                reservationId, account.getStatus());
        }
        
        // Release reserved balance
        account.setReservedBalance(getReservedBalance(account).subtract(reservation.getAmount()));
        // Restore daily transfer used
        account.setDailyTransferUsed(getDailyTransferUsed(account).subtract(reservation.getAmount()));
        accountRepository.save(account);
        
        // Update reservation status
        reservation.setStatus(BalanceReservation.ReservationStatus.ROLLED_BACK);
        reservationRepository.save(reservation);
        
        // Create statement
        statementService.createStatement(account.getId(), reservation.getTransactionId(),
            Statement.StatementType.RELEASE, reservation.getAmount(),
            account.getBalance(), "Balance released");
        
        log.info("Rolled back reservation: {} for account: {}", reservationId, account.getId());
    }

    private BigDecimal getDailyTransferLimit(Account account) {
        return account.getDailyTransferLimit() != null
            ? account.getDailyTransferLimit()
            : DEFAULT_DAILY_TRANSFER_LIMIT;
    }

    private BigDecimal getDailyTransferUsed(Account account) {
        return account.getDailyTransferUsed() != null
            ? account.getDailyTransferUsed()
            : ZERO;
    }

    private BigDecimal getReservedBalance(Account account) {
        return account.getReservedBalance() != null
            ? account.getReservedBalance()
            : ZERO;
    }
}
