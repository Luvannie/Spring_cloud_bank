package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.TransferResponse;
import com.banking.transaction.service.saga.TransferSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for initiating and managing transfer operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    
    private final TransferSaga transferSaga;
    private final TransactionService transactionService;
    
    /**
     * Initiate a new transfer operation.
     *
     * @param request the transfer request
     * @return the transfer response with transaction and saga details
     */
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer from {} to {}, amount: {} VND",
            request.getSourceAccountId(),
            request.getTargetAccountId() != null ? request.getTargetAccountId() : request.getTargetAccountNumber(),
            request.getAmount());
        
        validateTransferRequest(request);
        
        TransferResponse response = transferSaga.execute(request);
        
        log.info("Transfer saga {} completed with status: {}", 
            response.getSagaId(), response.getStatus());
        
        return response;
    }
    
    /**
     * Get the current status of a transfer.
     *
     * @param transactionId the transaction ID
     * @return the transfer response with current status
     */
    public TransferResponse getTransferStatus(UUID transactionId) {
        TransactionResponse txnResponse = transactionService.getTransactionResponse(transactionId);
        return TransferResponse.builder()
            .transactionId(txnResponse.getId())
            .sagaId(txnResponse.getSagaId())
            .referenceNumber(txnResponse.getReferenceNumber())
            .status(TransferResponse.TransferStatus.PROCESSING)
            .amount(txnResponse.getAmount())
            .message("Transfer is being processed")
            .build();
    }
    
    /**
     * Validate transfer request data.
     */
    private void validateTransferRequest(TransferRequest request) {
        if (request.getTargetAccountId() == null && 
            (request.getTargetAccountNumber() == null || request.getTargetAccountNumber().isBlank())) {
            throw new IllegalArgumentException(
                "Either targetAccountId or targetAccountNumber must be provided");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}