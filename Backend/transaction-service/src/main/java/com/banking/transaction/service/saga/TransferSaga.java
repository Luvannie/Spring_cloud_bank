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
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
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
            // Execute saga steps
            executeSagaSteps(sagaId, request, response);
            
            // Mark transaction as completed
            transactionService.markCompleted(transaction.getId());
            response.setStatus(TransferResponse.TransferStatus.COMPLETED);
            response.setMessage("Transfer completed successfully");
            
            // Update saga state
            sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
            sagaState.setCompletedAt(Instant.now());
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            log.error("Transfer saga failed: {}", sagaId, e);
            handleSagaFailure(sagaId, request, response, e);
        }
        
        return response;
    }
    
    /**
     * Execute all saga steps sequentially.
     */
    private void executeSagaSteps(UUID sagaId, TransferRequest request, TransferResponse response) {
        SagaState sagaState = sagaStateRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new com.banking.common.exception.BankingException(
                "SAGA_NOT_FOUND", "Saga not found"));
        
        for (int i = sagaState.getCurrentStep(); i < STEPS.size(); i++) {
            SagaStep step = STEPS.get(i);
            log.info("Executing saga step {}: {}", sagaId, step.getStepName());
            
            try {
                executeStep(step, request, response);
                
                // Update saga state
                sagaState.setCurrentStep(i + 1);
                updateStepsCompleted(sagaState, step, "OK");
                sagaStateRepository.save(sagaState);
                
            } catch (Exception e) {
                log.error("Saga step {} failed: {}", sagaId, step.getStepName(), e);
                sagaState.setLastError(e.getMessage());
                sagaStateRepository.save(sagaState);
                throw e;
            }
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
        
        if (transaction.getStatus() == Transaction.TransactionStatus.PROCESSING) {
            // Continue saga from step 4 (CONFIRM_TRANSFER)
            executeStepAfterPayment(transaction);
        }
    }
    
    /**
     * Execute remaining steps after payment confirmation.
     */
    private void executeStepAfterPayment(Transaction transaction) {
        SagaState sagaState = transaction.getSagaId() != null
            ? sagaStateRepository.findBySagaId(transaction.getSagaId()).orElse(null)
            : null;
        
        if (sagaState != null) {
            // Execute remaining steps
            TransferRequest request = fromJson(sagaState.getPayload(), TransferRequest.class);
            TransferResponse response = TransferResponse.builder()
                .transactionId(transaction.getId())
                .sagaId(transaction.getSagaId())
                .amount(request.getAmount())
                .build();
            
            // Execute step 4 (CONFIRM_TRANSFER)
            accountService.commitReservation(
                request.getSourceAccountId(), transaction.getId());
            
            // Execute step 5 (SEND_NOTIFICATIONS)
            notificationService.sendTransferConfirmation(request, response);
            
            // Complete saga
            transactionService.markCompleted(transaction.getId());
            sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
            sagaState.setCompletedAt(Instant.now());
            sagaStateRepository.save(sagaState);
        }
    }
    
    /**
     * Handle saga failure and execute compensation.
     */
    private void handleSagaFailure(UUID sagaId, TransferRequest request,
                                   TransferResponse response, Exception e) {
        SagaState sagaState = sagaStateRepository.findBySagaId(sagaId).orElse(null);
        
        if (sagaState != null) {
            sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
            sagaStateRepository.save(sagaState);
            
            // Execute compensation actions in reverse order
            compensate(sagaId, request);
            
            sagaState.setStatus(SagaState.SagaStatus.FAILED);
            sagaStateRepository.save(sagaState);
        }
        
        transactionService.markFailed(response.getTransactionId(), e.getMessage());
        response.setStatus(TransferResponse.TransferStatus.FAILED);
        response.setMessage("Transfer failed: " + e.getMessage());
    }
    
    /**
     * Execute compensation actions in reverse order.
     */
    private void compensate(UUID sagaId, TransferRequest request) {
        log.info("Executing compensation for saga: {}", sagaId);
        // Execute compensation actions in reverse order
        // This would call accountService.rollbackReservation, paymentService.cancelPayment, etc.
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
            steps.add(Map.of("step", step.getStepNumber(), "status", status));
            state.setStepsCompleted(objectMapper.writeValueAsString(steps));
        } catch (Exception e) {
            log.error("Failed to update steps completed", e);
        }
    }
}