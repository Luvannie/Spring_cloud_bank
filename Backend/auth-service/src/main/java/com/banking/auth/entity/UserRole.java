package com.banking.auth.entity;

import com.banking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * User-Role mapping entity for many-to-many relationship.
 */
@Entity
@Table(name = "user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}
