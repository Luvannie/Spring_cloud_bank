package com.banking.account.repository;

import com.banking.account.entity.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Statement entity operations.
 */
@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {
    
    /**
     * Find all statements for an account ordered by creation date descending.
     *
     * @param accountId the account ID
     * @return list of statements newest first
     */
    List<Statement> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    
    /**
     * Find statements for an account with pagination.
     *
     * @param accountId the account ID
     * @param pageable pagination parameters
     * @return page of statements
     */
    Page<Statement> findByAccountId(UUID accountId, Pageable pageable);
    
    /**
     * Find statement by transaction ID for idempotency checks.
     *
     * @param transactionId the transaction ID
     * @return optional statement if found
     */
    Optional<Statement> findByTransactionId(UUID transactionId);
}
