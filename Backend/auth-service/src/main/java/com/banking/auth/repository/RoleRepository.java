package com.banking.auth.repository;

import com.banking.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    Optional<Role> findByName(String name);
    
    List<Role> findByEnabled(Boolean enabled);
    
    /**
     * Find all roles for a specific user.
     * @param userId the user ID
     * @return list of roles assigned to the user
     */
    @Query("SELECT r FROM UserRole ur JOIN ur.role r WHERE ur.userId = :userId")
    List<Role> findByUserId(@Param("userId") UUID userId);
}
