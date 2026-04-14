package com.banking.transaction.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.TransferResponse;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {
    
    private final TransferService transferService;
    private final TransactionService transactionService;
    
    /**
     * Initiate a new transfer.
     *
     * @param request the transfer request
     * @param idempotencyKey optional idempotency key for deduplication
     * @return the transfer response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("Initiating transfer from {} to {}, amount: {}",
            request.getSourceAccountId(),
            request.getTargetAccountId() != null ? request.getTargetAccountId() : request.getTargetAccountNumber(),
            request.getAmount());
        
        if (idempotencyKey != null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        
        TransferResponse response = transferService.initiateTransfer(request);
        return ApiResponse.success(response);
    }
    
    /**
     * Get transfer status by transaction ID.
     *
     * @param transactionId the transaction ID
     * @return the transfer status response
     */
    @GetMapping("/{transactionId}/status")
    public ApiResponse<TransferResponse> getTransferStatus(@PathVariable UUID transactionId) {
        log.info("Getting transfer status for transaction: {}", transactionId);
        return ApiResponse.success(transferService.getTransferStatus(transactionId));
    }
}