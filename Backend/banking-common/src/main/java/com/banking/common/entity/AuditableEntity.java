package com.banking.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base entity with audit fields (createdAt, updatedAt).
 * Inherits id and version from BaseEntity.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class AuditableEntity extends BaseEntity {
    
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}