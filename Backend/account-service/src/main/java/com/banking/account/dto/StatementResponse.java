package com.banking.account.dto;

import com.banking.account.entity.Statement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for account statement entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementResponse {
    
    private UUID id;
    private UUID accountId;
    private UUID transactionId;
    private Statement.StatementType statementType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private Instant createdAt;
    
    /**
     * Creates StatementResponse from Statement entity.
     */
    public static StatementResponse from(Statement statement) {
        return StatementResponse.builder()
            .id(statement.getId())
            .accountId(statement.getAccountId())
            .transactionId(statement.getTransactionId())
            .statementType(statement.getStatementType())
            .amount(statement.getAmount())
            .balanceBefore(statement.getBalanceBefore())
            .balanceAfter(statement.getBalanceAfter())
            .description(statement.getDescription())
            .createdAt(statement.getCreatedAt())
            .build();
    }
}