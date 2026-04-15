package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Transaction entity operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    /**
     * Find a transaction by its idempotency key to prevent duplicate processing.
     *
     * @param idempotencyKey the unique idempotency key
     * @return optional transaction if found
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find a transaction by its reference number.
     *
     * @param referenceNumber the unique reference number
     * @return optional transaction if found
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    
    /**
     * Find all transactions where the specified account is the source.
     *
     * @param sourceAccountId the source account UUID
     * @return list of transactions
     */
    List<Transaction> findBySourceAccountId(UUID sourceAccountId);
    
    /**
     * Find all transactions where the specified account is the target.
     *
     * @param targetAccountId the target account UUID
     * @return list of transactions
     */
    List<Transaction> findByTargetAccountId(UUID targetAccountId);
    
    /**
     * Find transactions by source account with pagination, ordered by creation date descending.
     *
     * @param sourceAccountId the source account UUID
     * @param pageable        pagination information
     * @return page of transactions
     */
    Page<Transaction> findBySourceAccountIdOrderByCreatedAtDesc(UUID sourceAccountId, Pageable pageable);

    /**
     * Find transactions for multiple source accounts with pagination.
     */
    Page<Transaction> findBySourceAccountIdInOrderByCreatedAtDesc(List<UUID> sourceAccountIds, Pageable pageable);
    
    /**
     * Find all transactions with any of the specified statuses.
     *
     * @param statuses list of transaction statuses to filter by
     * @return list of matching transactions
     */
    List<Transaction> findByStatusIn(List<Transaction.TransactionStatus> statuses);
}
