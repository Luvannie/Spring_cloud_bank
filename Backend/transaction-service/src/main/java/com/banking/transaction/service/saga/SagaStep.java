package com.banking.transaction.service.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Definition of a saga step with its compensation action.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStep {
    
    private int stepNumber;
    private String stepName;
    private String compensateAction;
    private Class<?> inputType;
    private Class<?> compensateInputType;
}