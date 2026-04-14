package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.ReserveBalanceRequest;
import com.banking.account.dto.ReserveBalanceResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.BalanceReservation;
import com.banking.account.service.AccountService;
import com.banking.account.service.BalanceService;
import com.banking.common.dto.ApiResponse;
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
    
    /**
     * Creates a new account.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Account account = accountService.createAccount(request, user.getUsername());
        return ApiResponse.success(AccountResponse.from(account));
    }
    
    /**
     * Gets account by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable UUID id) {
        Account account = accountService.getAccount(id);
        return ApiResponse.success(AccountResponse.from(account));
    }
    
    /**
     * Gets account balance.
     */
    @GetMapping("/{id}/balance")
    public ApiResponse<BalanceResponse> getBalance(@PathVariable UUID id) {
        Account account = accountService.getBalance(id);
        return ApiResponse.success(BalanceResponse.builder()
            .accountId(account.getId())
            .balance(account.getBalance())
            .reservedBalance(account.getReservedBalance())
            .availableBalance(account.getAvailableBalance())
            .currency(account.getCurrency())
            .build());
    }
    
    /**
     * Freezes an account.
     */
    @PutMapping("/{id}/freeze")
    public ApiResponse<Void> freezeAccount(@PathVariable UUID id) {
        accountService.freezeAccount(id);
        return ApiResponse.success(null);
    }
    
    /**
     * Reserves balance for a transaction.
     */
    @PostMapping("/{id}/reserve")
    public ApiResponse<ReserveBalanceResponse> reserveBalance(
            @PathVariable UUID id,
            @Valid @RequestBody ReserveBalanceRequest request) {
        BalanceReservation reservation = balanceService.reserveBalance(id, 
            request.getTransactionId(), request.getAmount());
        return ApiResponse.success(ReserveBalanceResponse.from(reservation));
    }
    
    /**
     * Commits a balance reservation.
     */
    @PostMapping("/{id}/commit")
    public ApiResponse<Void> commitReservation(
            @PathVariable UUID id,
            @RequestParam UUID reservationId) {
        balanceService.commitReservation(reservationId);
        return ApiResponse.success(null);
    }
    
    /**
     * Rolls back a balance reservation.
     */
    @PostMapping("/{id}/rollback")
    public ApiResponse<Void> rollbackReservation(
            @PathVariable UUID id,
            @RequestParam UUID reservationId) {
        balanceService.rollbackReservation(reservationId);
        return ApiResponse.success(null);
    }
}