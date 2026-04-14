package com.banking.account.entity;

import com.banking.common.entity.BaseEntity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Balance reservation entity for Saga transaction coordination.
 * Tracks temporarily reserved funds during multi-step transactions.
 * Extends BaseEntity for id and version fields.
 */
@Entity
@Table(name = "balance_reservations")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceReservation extends BaseEntity {
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    /**
     * Reservation status for Saga transaction lifecycle.
     */
    public enum ReservationStatus {
        ACTIVE,
        COMMITTED,
        ROLLED_BACK
    }
}
