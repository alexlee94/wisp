package com.wisp.repository;

import com.wisp.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionId(String sessionId);
    List<UserSession> findByUserId(Long userId);
    List<UserSession> findByLastHeartbeatBefore(Instant cutoff);
    void deleteBySessionId(String sessionId);
    long countByUserId(Long userId);
}
