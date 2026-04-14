package com.banking.account.repository;

import com.banking.account.entity.BalanceReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for BalanceReservation entity operations.
 */
@Repository
public interface BalanceReservationRepository extends JpaRepository<BalanceReservation, UUID> {
    
    /**
     * Find all active reservations for an account.
     *
     * @param accountId the account ID
     * @param status the reservation status to filter by
     * @return list of matching reservations
     */
    List<BalanceReservation> findByAccountIdAndStatus(UUID accountId, BalanceReservation.ReservationStatus status);
    
    /**
     * Find all reservations for a specific transaction.
     *
     * @param transactionId the transaction ID
     * @return list of reservations for the transaction
     */
    List<BalanceReservation> findByTransactionId(UUID transactionId);
    
    /**
     * Expire all active reservations that have passed their expiration time.
     * This is a bulk update operation.
     *
     * @param now the current instant to compare against
     * @param status the status to set (typically ROLLED_BACK)
     * @return number of reservations updated
     */
    @Modifying
    @Query("UPDATE BalanceReservation r SET r.status = :status WHERE r.expiresAt < :now AND r.status = 'ACTIVE'")
    int expireReservations(@Param("now") Instant now, @Param("status") BalanceReservation.ReservationStatus status);
}
