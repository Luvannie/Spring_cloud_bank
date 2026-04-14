package com.banking.transaction.entity;

import com.banking.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga state entity for tracking distributed transaction orchestration state.
 */
@Entity
@Table(name = "saga_states")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState extends AuditableEntity {
    
    @Column(name = "saga_id", nullable = false, unique = true)
    private UUID sagaId;
    
    @Column(name = "saga_type", nullable = false)
    private String sagaType;
    
    @Column(name = "current_step", nullable = false)
    private Integer currentStep;
    
    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;
    
    @Column(columnDefinition = "jsonb")
    private String payload;
    
    @Column(columnDefinition = "jsonb")
    private String stepsCompleted;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;
    
    @Column(name = "last_error")
    private String lastError;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Saga status enum representing the state of a saga orchestration.
     */
    public enum SagaStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        COMPENSATING
    }
}
