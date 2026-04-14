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
 * Role entity for user authorization.
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private Boolean enabled;
}
