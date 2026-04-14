package com.banking.auth.repository;

import com.banking.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Session entity operations.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    
    Optional<Session> findBySessionId(String sessionId);
    
    Optional<Session> findByUserIdAndRevokedFalse(String userId);
    
    List<Session> findByUserId(UUID userId);
    
    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.userId = :userId")
    int revokeAllUserSessions(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") Instant now);
}
