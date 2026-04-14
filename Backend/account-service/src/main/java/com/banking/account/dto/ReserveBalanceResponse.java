package com.banking.account.dto;

import com.banking.account.entity.BalanceReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for balance reservation operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReserveBalanceResponse {
    
    private UUID id;
    private UUID accountId;
    private UUID transactionId;
    private BigDecimal amount;
    private BalanceReservation.ReservationStatus status;
    private Instant expiresAt;
    
    /**
     * Creates ReserveBalanceResponse from BalanceReservation entity.
     */
    public static ReserveBalanceResponse from(BalanceReservation reservation) {
        return ReserveBalanceResponse.builder()
            .id(reservation.getId())
            .accountId(reservation.getAccountId())
            .transactionId(reservation.getTransactionId())
            .amount(reservation.getAmount())
            .status(reservation.getStatus())
            .expiresAt(reservation.getExpiresAt())
            .build();
    }
}