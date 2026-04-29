package com.audiocall.dto;

import com.audiocall.model.SessionMode;
import com.audiocall.model.SessionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionStats {
    private Long sessionId;
    private String title;
    private SessionMode mode;
    private SessionStatus status;
    private int totalNumbers;
    private int queued;
    private int ringing;
    private int connected;
    private int completed;
    private int failed;
    private int busy;
    private int noAnswer;
    private LocalDateTime startedAt;
    private long durationSeconds;
}
