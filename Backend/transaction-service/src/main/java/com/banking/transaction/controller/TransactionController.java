package com.banking.transaction.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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