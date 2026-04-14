package com.banking.account.repository;

import com.banking.account.entity.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Account entity operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    
    /**
     * Find account by unique account number.
     *
     * @param accountNumber the account number to search
     * @return optional account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);
    
    /**
     * Find all accounts for a specific user.
     *
     * @param userId the user ID
     * @return list of accounts belonging to the user
     */
    List<Account> findByUserId(UUID userId);
    
    /**
     * Find all accounts with a specific status.
     *
     * @param status the account status to filter by
     * @return list of accounts with the given status
     */
    List<Account> findByStatus(Account.AccountStatus status);
    
    /**
     * Find account by ID with pessimistic write lock for financial operations.
     * This prevents concurrent reads during updates.
     *
     * @param id the account ID
     * @return optional account if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);
}
