package com.audiocall.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {
    private long totalPhoneNumbers;
    private long activePhoneNumbers;
    private long totalSessions;
    private long activeSessions;
    private long callsToday;
    private long completedCallsToday;
    private double successRate;
}
