package com.banking.account.dto;

import com.banking.account.entity.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {
    
    @NotNull
    private UUID userId;
    
    @NotNull
    private Account.AccountType accountType;
    
    @NotBlank
    private String firstName;
    
    @NotBlank
    private String lastName;
    
    @Builder.Default
    private String currency = "VND";
}