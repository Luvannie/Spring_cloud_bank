package com.banking.transaction.service.saga;

import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.TransferResponse;
import com.banking.transaction.service.TransactionService;
import com.banking.transaction.service.proxy.AccountServiceProxy;
import com.banking.transaction.service.proxy.NotificationServiceProxy;
import com.banking.transaction.service.proxy.PaymentServiceProxy;
import com.banking.transaction.entity.SagaState;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.repository.SagaStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga orchestrator for transfer operations.
 * Handles distributed transaction coordination with compensation support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSaga {
    
    private final TransactionService transactionService;
    private final AccountServiceProxy accountService;
    private final PaymentServiceProxy paymentService;
    private final NotificationServiceProxy notificationService;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;
    
    private static final String SAGA_TYPE = "TRANSFER_SAGA";
    
    // Saga steps definition
    private static final List<SagaStep> STEPS = List.of(
        SagaStep.builder()
            .stepNumber(1)
            .stepName("RESERVE_BALANCE")
            .compensateAction("RELEASE_BALANCE")
            .build(),
        SagaStep.builder()
            .stepNumber(2)
            .stepName("CREATE_PAYMENT_LINK")
            .compensateAction("CANCEL_PAYMENT")
            .build(),
        SagaStep.builder()
            .stepNumber(3)
            .stepName("WAIT_FOR_PAYMENT")
            .compensateAction("CANCEL_PAYMENT")
            .build(),
        SagaStep.builder()
            .stepNumber(4)
            .stepName("CONFIRM_TRANSFER")
            .compensateAction("REFUND_TRANSFER")
            .build(),
        SagaStep.builder()
            .stepNumber(5)
            .stepName("SEND_NOTIFICATIONS")
            .compensateAction("SEND_COMPENSATION_NOTIFICATION")
            .build()
    );
    
    /**
     * Execute the transfer saga.
     *
     * @param request the transfer request
     * @return the transfer response
     */
    @Transactional
    public TransferResponse execute(TransferRequest request) {
        UUID sagaId = UUID.randomUUID();
        log.info("Starting transfer saga: {} for transaction: {}", sagaId, request.getSourceAccountId());
        
        // Create saga state
        SagaState sagaState = SagaState.builder()
            .sagaId(sagaId)
            .sagaType(SAGA_TYPE)
            .currentStep(0)
            .totalSteps(STEPS.size())
            .status(SagaState.SagaStatus.RUNNING)
            .payload(toJson(request))
            .stepsCompleted("[]")
            .retryCount(0)
            .maxRetries(3)
            .startedAt(Instant.now())
            .build();
        
        sagaStateRepository.save(sagaState);
        
        // Create transaction record
        Transaction transaction = transactionService.createPendingTransaction(sagaId, request);
        
        TransferResponse response = TransferResponse.builder()
            .transactionId(transaction.getId())
            .sagaId(sagaId)
            .referenceNumber(transaction.getReferenceNumber())
            .status(TransferResponse.TransferStatus.PROCESSING)
            .amount(request.getAmount())
            .build();
        
        try {
            executeInitialSteps(sagaState, request, response);
            transactionService.markProcessing(transaction.getId());
            response.setStatus(TransferResponse.TransferStatus.PROCESSING);
            response.setMessage("Transfer initiated. Waiting for payment confirmation");
        } catch (Exception e) {
            log.error("Transfer saga failed: {}", sagaId, e);
            handleSagaFailure(sagaId, request, transaction.getId(), response, e);
        }
        
        return response;
    }
    
    /**
     * Execute the synchronous steps before waiting for payment confirmation.
     */
    private void executeInitialSteps(SagaState sagaState, TransferRequest request, TransferResponse response) {
        executeAndPersistStep(sagaState, STEPS.get(0), request, response, "OK");
        executeAndPersistStep(sagaState, STEPS.get(1), request, response, "OK");

        SagaStep waitForPayment = STEPS.get(2);
        log.info("Saga {} is waiting for payment confirmation for transaction: {}",
            sagaState.getSagaId(), response.getTransactionId());
        sagaState.setCurrentStep(3);
        updateStepsCompleted(sagaState, waitForPayment, "WAITING_FOR_PAYMENT");
        sagaStateRepository.save(sagaState);
    }

    /**
     * Continue the saga after payment confirmation.
     */
    private void executePostPaymentSteps(SagaState sagaState, TransferRequest request, TransferResponse response) {
        for (int index = sagaState.getCurrentStep(); index < STEPS.size(); index++) {
            executeAndPersistStep(sagaState, STEPS.get(index), request, response, "OK");
        }
    }

    private void executeAndPersistStep(
            SagaState sagaState,
            SagaStep step,
            TransferRequest request,
            TransferResponse response,
            String status) {
        try {
            log.info("Executing saga step {}: {}", sagaState.getSagaId(), step.getStepName());
            executeStep(step, request, response);
            sagaState.setCurrentStep(step.getStepNumber());
            sagaState.setLastError(null);
            updateStepsCompleted(sagaState, step, status);
            sagaStateRepository.save(sagaState);
        } catch (Exception e) {
            log.error("Saga step {} failed: {}", sagaState.getSagaId(), step.getStepName(), e);
            sagaState.setLastError(e.getMessage());
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    /**
     * Execute a single saga step.
     */
    private void executeStep(SagaStep step, TransferRequest request, TransferResponse response) {
        switch (step.getStepName()) {
            case "RESERVE_BALANCE" -> {
                accountService.reserveBalance(request.getSourceAccountId(),
                    response.getTransactionId(), request.getAmount());
            }
            case "CREATE_PAYMENT_LINK" -> {
                UUID paymentId = paymentService.createPaymentLink(
                    response.getTransactionId(), request.getAmount(), request.getDescription());
                response.setMessage("Payment link created: " + paymentId);
            }
            case "WAIT_FOR_PAYMENT" -> {
                // This step is handled asynchronously via webhook
                log.info("Waiting for payment confirmation for saga: {}", response.getTransactionId());
            }
            case "CONFIRM_TRANSFER" -> {
                accountService.commitReservation(request.getSourceAccountId(),
                    response.getTransactionId());
            }
            case "SEND_NOTIFICATIONS" -> {
                notificationService.sendTransferConfirmation(request, response);
            }
        }
    }
    
    /**
     * Handle payment success webhook from Kafka.
     *
     * @param transactionId the transaction ID
     * @param paymentId the payment ID
     */
    @Transactional
    public void handlePaymentSuccess(UUID transactionId, UUID paymentId) {
        log.info("Payment successful for transaction: {}, payment: {}", transactionId, paymentId);
        
        Transaction transaction = transactionService.findById(transactionId)
            .orElseThrow(() -> new com.banking.common.exception.BankingException(
                "TRANSACTION_NOT_FOUND", "Transaction not found"));

        if (transaction.getStatus() != Transaction.TransactionStatus.PROCESSING) {
            log.info("Ignoring payment success for transaction {} in status {}",
                transactionId, transaction.getStatus());
            return;
        }

        SagaState sagaState = requireSagaState(transaction.getSagaId());
        TransferRequest request = fromJson(sagaState.getPayload(), TransferRequest.class);
        TransferResponse response = buildResponse(transaction, request);

        try {
            executePostPaymentSteps(sagaState, request, response);
            transactionService.markCompleted(transaction.getId());
            sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
            sagaState.setCompletedAt(Instant.now());
            sagaStateRepository.save(sagaState);
            log.info("Transfer saga {} completed after payment success", sagaState.getSagaId());
        } catch (Exception e) {
            handleSagaFailure(sagaState.getSagaId(), request, transactionId, response, e);
        }
    }
    
    /**
     * Handle payment failure or expiration by compensating the saga.
     */
    @Transactional
    public void handlePaymentFailure(UUID transactionId, String reason) {
        log.warn("Payment failed for transaction {}: {}", transactionId, reason);

        Transaction transaction = transactionService.findById(transactionId)
            .orElseThrow(() -> new com.banking.common.exception.BankingException(
                "TRANSACTION_NOT_FOUND", "Transaction not found"));

        if (transaction.getStatus() == Transaction.TransactionStatus.COMPLETED
                || transaction.getStatus() == Transaction.TransactionStatus.FAILED
                || transaction.getStatus() == Transaction.TransactionStatus.CANCELLED) {
            log.info("Ignoring payment failure for transaction {} in terminal status {}",
                transactionId, transaction.getStatus());
            return;
        }

        SagaState sagaState = requireSagaState(transaction.getSagaId());
        TransferRequest request = fromJson(sagaState.getPayload(), TransferRequest.class);
        TransferResponse response = buildResponse(transaction, request);
        handleSagaFailure(sagaState.getSagaId(), request, transactionId, response,
            new IllegalStateException("Payment failed: " + reason));
    }
    
    /**
     * Handle saga failure and execute compensation.
     */
    private void handleSagaFailure(UUID sagaId, TransferRequest request, UUID transactionId,
                                   TransferResponse response, Exception e) {
        SagaState sagaState = sagaStateRepository.findBySagaId(sagaId).orElse(null);
        
        if (sagaState != null) {
            sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
            sagaState.setLastError(e.getMessage());
            sagaStateRepository.save(sagaState);
            
            // Execute compensation actions in reverse order
            compensate(sagaId, request, transactionId);
            
            sagaState.setStatus(SagaState.SagaStatus.FAILED);
            sagaStateRepository.save(sagaState);
        }
        
        transactionService.markFailed(transactionId, e.getMessage());
        notificationService.sendCompensationNotification(request, e.getMessage());
        response.setStatus(TransferResponse.TransferStatus.FAILED);
        response.setMessage("Transfer failed: " + e.getMessage());
    }
    
    /**
     * Execute compensation actions in reverse order.
     */
    private void compensate(UUID sagaId, TransferRequest request, UUID transactionId) {
        log.info("Executing compensation for saga: {}", sagaId);
        
        SagaState sagaState = sagaStateRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new com.banking.common.exception.BankingException(
                "SAGA_NOT_FOUND", "Saga not found"));
        
        // Parse completed steps from JSON
        List<Map<String, Object>> completedSteps = parseStepsCompleted(sagaState.getStepsCompleted());
        
        // Execute compensation in REVERSE order
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            Map<String, Object> step = completedSteps.get(i);
            String stepName = (String) step.get("stepName");
            
            try {
                executeCompensation(stepName, request, transactionId);
                log.info("Compensation successful for step: {} in saga: {}", stepName, sagaId);
            } catch (Exception e) {
                log.error("Compensation failed for step: {} in saga: {}", stepName, sagaId, e);
                // Mark saga as requiring manual intervention
                sagaState.setStatus(SagaState.SagaStatus.COMPENSATION_FAILED);
                sagaState.setLastError("Compensation failed for step " + stepName + ": " + e.getMessage());
                sagaStateRepository.save(sagaState);
                return;
            }
        }
        
        sagaState.setStatus(SagaState.SagaStatus.COMPENSATED);
        sagaStateRepository.save(sagaState);
        log.info("Compensation completed for saga: {}", sagaId);
    }
    
    /**
     * Parse completed steps from JSON string.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseStepsCompleted(String stepsJson) {
        try {
            return objectMapper.readValue(stepsJson, 
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse steps completed", e);
            return List.of();
        }
    }
    
    /**
     * Execute compensation for a specific step.
     */
    private void executeCompensation(String stepName, TransferRequest request, UUID transactionId) {
        switch (stepName) {
            case "RESERVE_BALANCE" -> {
                // Rollback the balance reservation
                log.info("Compensating RESERVE_BALANCE - releasing reserved balance");
                accountService.rollbackReservation(
                    request.getSourceAccountId(), 
                    transactionId);
            }
            case "CREATE_PAYMENT_LINK", "WAIT_FOR_PAYMENT" -> {
                // Cancel the payment
                log.info("Compensating {} - canceling payment", stepName);
                paymentService.cancelPayment(transactionId);
            }
            case "CONFIRM_TRANSFER" -> {
                // For confirmed transfers, this may require manual review
                log.warn("CONFIRM_TRANSFER compensation may require manual intervention for transaction: {}", 
                    transactionId);
                // In production, this would trigger an alert for manual reconciliation
            }
            case "SEND_NOTIFICATIONS" -> {
                // Notification compensation usually not needed
                log.debug("No compensation needed for SEND_NOTIFICATIONS step");
            }
        }
    }

    private SagaState requireSagaState(UUID sagaId) {
        return sagaStateRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new com.banking.common.exception.BankingException(
                "SAGA_NOT_FOUND", "Saga not found"));
    }

    private TransferResponse buildResponse(Transaction transaction, TransferRequest request) {
        return TransferResponse.builder()
            .transactionId(transaction.getId())
            .sagaId(transaction.getSagaId())
            .referenceNumber(transaction.getReferenceNumber())
            .status(TransferResponse.TransferStatus.PROCESSING)
            .amount(request.getAmount())
            .build();
    }
    
    /**
     * Marks a transaction as failed with error message.
     */
    private void markFailed(UUID transactionId, String errorMessage) {
        transactionService.markFailed(transactionId, errorMessage);
    }
    
    /**
     * Serialize object to JSON string.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new com.banking.common.exception.BankingException("JSON_ERROR", 
                "Failed to serialize request");
        }
    }
    
    /**
     * Deserialize JSON string to object.
     */
    private TransferRequest fromJson(String json, Class<TransferRequest> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new com.banking.common.exception.BankingException("JSON_ERROR",
                "Failed to deserialize request");
        }
    }
    
    /**
     * Update steps completed JSON array.
     */
    private void updateStepsCompleted(SagaState state, SagaStep step, String status) {
        try {
            List<Map<String, Object>> steps = objectMapper.readValue(
                state.getStepsCompleted(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );
            steps.add(Map.of(
                "step", step.getStepNumber(), 
                "stepName", step.getStepName(),
                "status", status));
            state.setStepsCompleted(objectMapper.writeValueAsString(steps));
        } catch (Exception e) {
            log.error("Failed to update steps completed", e);
        }
    }
}
