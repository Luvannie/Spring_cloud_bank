package com.banking.auth.service;

import com.banking.auth.entity.Session;
import com.banking.auth.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for session management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final SessionRepository sessionRepository;
    
    /**
     * Creates a new session for a user.
     */
    @Transactional
    public Session createSession(UUID userId, String sessionId, String refreshToken, 
                                  Instant expiresAt, String ipAddress) {
        Session session = Session.builder()
            .userId(userId)
            .sessionId(sessionId)
            .refreshToken(refreshToken)
            .expiresAt(expiresAt)
            .createdAt(Instant.now())
            .ipAddress(ipAddress)
            .revoked(false)
            .build();
        
        return sessionRepository.save(session);
    }
    
    /**
     * Finds a session by session ID.
     */
    public Optional<Session> findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }
    
    /**
     * Finds an active (non-revoked) session for a user.
     */
    public Optional<Session> findActiveSessionByUserId(String userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }
    
    /**
     * Finds all sessions for a user.
     */
    public List<Session> findByUserId(UUID userId) {
        return sessionRepository.findByUserId(userId);
    }
    
    /**
     * Revokes all sessions for a user.
     */
    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        int revoked = sessionRepository.revokeAllUserSessions(userId);
        log.info("Revoked {} sessions for user {}", revoked, userId);
    }
    
    /**
     * Updates the refresh token for a session.
     */
    @Transactional
    public void updateRefreshToken(UUID userId, String newRefreshToken) {
        sessionRepository.findByUserIdAndRevokedFalse(userId.toString())
            .ifPresent(session -> {
                session.setRefreshToken(newRefreshToken);
                sessionRepository.save(session);
            });
    }
    
    /**
     * Deletes expired sessions.
     */
    @Transactional
    public int deleteExpiredSessions() {
        int deleted = sessionRepository.deleteExpiredSessions(Instant.now());
        log.info("Deleted {} expired sessions", deleted);
        return deleted;
    }
    
    /**
     * Revokes a specific session.
     */
    @Transactional
    public void revokeSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setRevoked(true);
            sessionRepository.save(session);
        });
    }
}
