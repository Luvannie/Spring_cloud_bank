package com.banking.transaction.service.saga;

import com.banking.common.exception.BankingException;
import com.banking.transaction.entity.SagaState;
import com.banking.transaction.repository.SagaStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base orchestrator for saga pattern implementation.
 * Handles step execution, state management, and compensation logic.
 *
 * @param <T> the type of request handled by the saga
 */
@Component
@RequiredArgsConstructor
@Slf4j
public abstract class SagaOrchestrator<T> {
    
    protected final SagaStateRepository sagaStateRepository;
    protected final ObjectMapper objectMapper;
    
    /**
     * Execute the complete saga with the given request.
     *
     * @param sagaId unique saga identifier
     * @param request the saga request
     * @param steps the list of saga steps to execute
     * @return the result of saga execution
     */
    protected Object executeSaga(UUID sagaId, T request, List<SagaStep> steps) {
        log.info("Starting saga: {} with {} steps", sagaId, steps.size());
        
        SagaState sagaState = createSagaState(sagaId, steps, request);
        sagaStateRepository.save(sagaState);
        
        try {
            executeSteps(sagaId, request, steps, sagaState);
            completeSaga(sagaState);
            return buildSuccessResponse(sagaId, request);
        } catch (Exception e) {
            log.error("Saga {} failed: {}", sagaId, e.getMessage());
            return handleFailure(sagaId, request, e);
        }
    }
    
    /**
     * Create initial saga state.
     */
    private SagaState createSagaState(UUID sagaId, List<SagaStep> steps, T request) {
        return SagaState.builder()
            .sagaId(sagaId)
            .sagaType(getSagaType())
            .currentStep(0)
            .totalSteps(steps.size())
            .status(SagaState.SagaStatus.RUNNING)
            .payload(toJson(request))
            .stepsCompleted("[]")
            .retryCount(0)
            .maxRetries(getMaxRetries())
            .startedAt(Instant.now())
            .build();
    }
    
    /**
     * Execute saga steps sequentially.
     */
    private void executeSteps(UUID sagaId, T request, List<SagaStep> steps, SagaState state) {
        for (int i = state.getCurrentStep(); i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            log.info("Executing saga step {}: {}", sagaId, step.getStepName());
            
            try {
                executeStep(step, request);
                state.setCurrentStep(i + 1);
                updateStepsCompleted(state, step, "OK");
                sagaStateRepository.save(state);
            } catch (Exception e) {
                log.error("Saga step {} failed: {}", sagaId, step.getStepName(), e);
                state.setLastError(e.getMessage());
                sagaStateRepository.save(state);
                throw e;
            }
        }
    }
    
    /**
     * Execute a single saga step.
     */
    protected abstract void executeStep(SagaStep step, T request);
    
    /**
     * Build success response after saga completion.
     */
    protected abstract Object buildSuccessResponse(UUID sagaId, T request);
    
    /**
     * Handle saga failure and execute compensation.
     */
    private Object handleFailure(UUID sagaId, T request, Exception e) {
        SagaState sagaState = sagaStateRepository.findBySagaId(sagaId).orElse(null);
        
        if (sagaState != null) {
            sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
            sagaStateRepository.save(sagaState);
            
            compensate(sagaId, request);
            
            sagaState.setStatus(SagaState.SagaStatus.FAILED);
            sagaStateRepository.save(sagaState);
        }
        
        return buildFailureResponse(sagaId, request, e);
    }
    
    /**
     * Execute compensation actions in reverse order.
     */
    protected void compensate(UUID sagaId, T request) {
        log.info("Executing compensation for saga: {}", sagaId);
        // Implemented by concrete saga classes
    }
    
    /**
     * Mark saga as completed.
     */
    private void completeSaga(SagaState state) {
        state.setStatus(SagaState.SagaStatus.COMPLETED);
        state.setCompletedAt(Instant.now());
        sagaStateRepository.save(state);
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
    
    /**
     * Serialize request to JSON.
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BankingException("JSON_ERROR", "Failed to serialize request");
        }
    }
    
    /**
     * Deserialize JSON to request.
     */
    protected T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new BankingException("JSON_ERROR", "Failed to deserialize request");
        }
    }
    
    /**
     * Get the saga type identifier.
     */
    protected abstract String getSagaType();
    
    /**
     * Get maximum retry count for saga steps.
     */
    protected int getMaxRetries() {
        return 3;
    }
    
    /**
     * Build failure response after saga fails.
     */
    protected abstract Object buildFailureResponse(UUID sagaId, T request, Exception e);
}