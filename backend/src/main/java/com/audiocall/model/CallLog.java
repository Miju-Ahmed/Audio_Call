package com.audiocall.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs", indexes = {
    @Index(name = "idx_call_logs_session", columnList = "session_id"),
    @Index(name = "idx_call_logs_status", columnList = "status"),
    @Index(name = "idx_call_logs_call_sid", columnList = "call_sid")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CallSession session;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "call_sid", length = 50)
    private String callSid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @Builder.Default
    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    private boolean muted = true;
}
