package com.wisp.service;

import com.wisp.entity.User;
import com.wisp.entity.UserSession;
import com.wisp.repository.UserRepository;
import com.wisp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final int MISSED_PINGS_BEFORE_OFFLINE = 2;

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Connect ───────────────────────────────────────────────────────────────

    @Transactional
    public void registerSession(Long userId, String sessionId) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .lastHeartbeat(Instant.now())
                .build();
        sessionRepository.save(session);

        // Only broadcast "online" if this is their first active session
        long activeSessions = sessionRepository.countByUserId(userId);
        if (activeSessions == 1) {
            setUserOnlineStatus(userId, true);
        }
    }

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    @Transactional
    public void recordHeartbeat(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setLastHeartbeat(Instant.now());
            sessionRepository.save(session);
        });
    }

    // ── Disconnect (explicit) ─────────────────────────────────────────────────

    @Transactional
    public void removeSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            Long userId = session.getUserId();
            sessionRepository.deleteBySessionId(sessionId);

            // Only flip offline if this was the LAST session (reference counting)
            long remaining = sessionRepository.countByUserId(userId);
            if (remaining == 0) {
                setUserOnlineStatus(userId, false);
            }
        });
    }

    // ── Stale session cleanup (the heartbeat mechanism) ──────────────────────

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_SECONDS * 1000L)
    @Transactional
    public void cleanupStaleSessions() {
        Instant cutoff = Instant.now().minus(
                HEARTBEAT_INTERVAL_SECONDS * MISSED_PINGS_BEFORE_OFFLINE,
                ChronoUnit.SECONDS
        );

        List<UserSession> staleSessions = sessionRepository.findByLastHeartbeatBefore(cutoff);

        for (UserSession session : staleSessions) {
            log.info("Session {} missed {} heartbeats, removing", session.getSessionId(), MISSED_PINGS_BEFORE_OFFLINE);
            removeSession(session.getSessionId());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setUserOnlineStatus(Long userId, boolean online) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(online);
            user.setLastSeen(Instant.now());
            userRepository.save(user);

            messagingTemplate.convertAndSend(
                    "/topic/presence",
                    new PresenceUpdate(userId, user.getUsername(), online)
            );
        });
    }

    private record PresenceUpdate(Long userId, String username, boolean online) {}
}
