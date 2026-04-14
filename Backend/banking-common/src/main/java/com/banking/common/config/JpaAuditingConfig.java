package com.banking.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing configuration.
 * Enables @PrePersist and @PreUpdate callbacks on AuditableEntity.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}