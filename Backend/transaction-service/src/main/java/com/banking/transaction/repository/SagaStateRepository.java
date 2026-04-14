package com.banking.transaction.repository;

import com.banking.transaction.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SagaState entity operations.
 */
@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    
    /**
     * Find a saga state by its unique saga ID.
     *
     * @param sagaId the unique saga identifier
     * @return optional saga state if found
     */
    Optional<SagaState> findBySagaId(UUID sagaId);
    
    /**
     * Find all saga states with the specified status.
     *
     * @param status the saga status to filter by
     * @return list of matching saga states
     */
    List<SagaState> findByStatus(SagaState.SagaStatus status);
    
    /**
     * Find saga states by status with retry count below a threshold.
     *
     * @param status    the saga status to filter by
     * @param maxRetries the maximum retry count threshold
     * @return list of matching saga states
     */
    List<SagaState> findByStatusAndRetryCountLessThan(SagaState.SagaStatus status, Integer maxRetries);
    
    /**
     * Find stale sagas that are still running or compensating but started before the threshold time.
     * These sagas may have crashed and need to be recovered or compensated.
     *
     * @param threshold the instant threshold to check against startedAt
     * @return list of stale saga states
     */
    @Query("SELECT s FROM SagaState s WHERE s.status IN ('RUNNING', 'COMPENSATING') AND s.startedAt < :threshold")
    List<SagaState> findStaleSagas(@Param("threshold") Instant threshold);
}
