package com.banking.payment.entity;

import com.banking.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment entity representing a payment transaction through PayOS.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Payment extends AuditableEntity {
    
    @EqualsAndHashCode.Include
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Column(name = "payos_order_code")
    private String payosOrderCode;
    
    @Column(name = "payos_transaction_id")
    private String payosTransactionId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(name = "qr_code_url")
    private String qrCodeUrl;
    
    @Column(name = "payment_url")
    private String paymentUrl;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "paid_at")
    private Instant paidAt;
    
    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
    }
}