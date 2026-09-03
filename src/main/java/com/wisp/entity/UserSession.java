package com.wisp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat;
}
