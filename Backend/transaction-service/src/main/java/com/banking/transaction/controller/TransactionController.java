package com.banking.transaction.controller;

import com.banking.account.repository.AccountRepository;
import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import com.banking.common.dto.ApiResponse;
import com.banking.common.dto.PageResponse;
import com.banking.common.exception.BankingException;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for transaction operations.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Lists transactions for the current user's accounts.
     */
    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new BankingException("USER_NOT_FOUND", "User not found"));

        List<UUID> accountIds = accountRepository.findByUserId(currentUser.getId())
            .stream()
            .map(account -> account.getId())
            .toList();

        if (accountIds.isEmpty()) {
            return ApiResponse.success(PageResponse.of(List.of(), page, size, 0));
        }

        Page<TransactionResponse> transactions = transactionService.getTransactionsForSourceAccounts(
            accountIds,
            PageRequest.of(page, size)
        );

        return ApiResponse.success(PageResponse.of(
            transactions.getContent(),
            transactions.getNumber(),
            transactions.getSize(),
            transactions.getTotalElements()
        ));
    }
    
    /**
     * Get transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return the transaction response
     */
    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        log.info("Getting transaction: {}", transactionId);
        return ApiResponse.success(transactionService.getTransactionResponse(transactionId));
    }
    
    /**
     * Get transaction by reference number.
     *
     * @param referenceNumber the reference number
     * @return the transaction response
     */
    @GetMapping("/reference/{referenceNumber}")
    public ApiResponse<TransactionResponse> getByReferenceNumber(@PathVariable String referenceNumber) {
        log.info("Getting transaction by reference: {}", referenceNumber);
        return transactionService.findByReferenceNumber(referenceNumber)
            .map(TransactionResponse::from)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("Transaction not found"));
    }
}
