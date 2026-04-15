package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.ReserveBalanceRequest;
import com.banking.account.dto.ReserveBalanceResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.BalanceReservation;
import com.banking.auth.entity.User;
import com.banking.account.service.AccountService;
import com.banking.account.service.BalanceService;
import com.banking.auth.repository.UserRepository;
import com.banking.common.dto.ApiResponse;
import com.banking.common.exception.BankingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for account operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    
    private final AccountService accountService;
    private final BalanceService balanceService;
    private final UserRepository userRepository;

    /**
     * Lists accounts owned by the current user.
     */
    @GetMapping
    public ApiResponse<List<AccountResponse>> getCurrentUserAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        List<AccountResponse> accounts = accountService.getAccountsForUser(currentUser.getId())
            .stream()
            .map(AccountResponse::from)
            .toList();
        return ApiResponse.success(accounts);
    }
    
    /**
     * Gets account by ID with ownership verification.
     */
    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        return ApiResponse.success(AccountResponse.from(account));
    }
    
    /**
     * Gets account balance with ownership verification.
     */
    @GetMapping("/{id}/balance")
    public ApiResponse<BalanceResponse> getBalance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getBalance(id);
        verifyAccountOwnership(account, userDetails);
        return ApiResponse.success(BalanceResponse.builder()
            .accountId(account.getId())
            .balance(account.getBalance())
            .reservedBalance(account.getReservedBalance())
            .availableBalance(account.getAvailableBalance())
            .currency(account.getCurrency())
            .build());
    }
    
    /**
     * Creates a new account with userId verification.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new BankingException("USER_NOT_FOUND", "User not found"));
        
        // Verify userId matches current user
        if (!request.getUserId().equals(currentUser.getId())) {
            throw new BankingException("FORBIDDEN", "Cannot create account for another user");
        }
        
        Account account = accountService.createAccount(request, userDetails.getUsername());
        return ApiResponse.success(AccountResponse.from(account));
    }
    
    /**
     * Freezes an account with ownership verification.
     */
    @PutMapping("/{id}/freeze")
    public ApiResponse<Void> freezeAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        accountService.freezeAccount(id);
        return ApiResponse.success(null);
    }
    
    /**
     * Unfreezes an account with ownership verification.
     */
    @PutMapping("/{id}/unfreeze")
    public ApiResponse<Void> unfreezeAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        accountService.unfreezeAccount(id);
        return ApiResponse.success(null);
    }
    
    /**
     * Reserves balance with ownership verification.
     */
    @PostMapping("/{id}/reserve")
    public ApiResponse<ReserveBalanceResponse> reserveBalance(
            @PathVariable UUID id,
            @Valid @RequestBody ReserveBalanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        BalanceReservation reservation = balanceService.reserveBalance(id, 
            request.getTransactionId(), request.getAmount());
        return ApiResponse.success(ReserveBalanceResponse.from(reservation));
    }
    
    /**
     * Commits a balance reservation with ownership verification.
     */
    @PostMapping("/{id}/commit")
    public ApiResponse<Void> commitReservation(
            @PathVariable UUID id,
            @RequestParam UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        balanceService.commitReservation(reservationId);
        return ApiResponse.success(null);
    }
    
    /**
     * Rolls back a balance reservation with ownership verification.
     */
    @PostMapping("/{id}/rollback")
    public ApiResponse<Void> rollbackReservation(
            @PathVariable UUID id,
            @RequestParam UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccount(id);
        verifyAccountOwnership(account, userDetails);
        balanceService.rollbackReservation(reservationId);
        return ApiResponse.success(null);
    }
    
    /**
     * Verifies that the current user owns the account.
     */
    private void verifyAccountOwnership(Account account, UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        if (!account.getUserId().equals(currentUser.getId())) {
            throw new BankingException("ACCESS_DENIED", "You don't have access to this account");
        }
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new BankingException("USER_NOT_FOUND", "User not found"));
    }
}
