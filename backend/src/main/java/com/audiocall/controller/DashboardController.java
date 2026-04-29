package com.audiocall.controller;

import com.audiocall.dto.DashboardStats;
import com.audiocall.model.SessionStatus;
import com.audiocall.repository.CallLogRepository;
import com.audiocall.repository.CallSessionRepository;
import com.audiocall.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PhoneNumberRepository phoneNumberRepository;
    private final CallSessionRepository sessionRepository;
    private final CallLogRepository callLogRepository;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long totalPhoneNumbers = phoneNumberRepository.count();
        long activePhoneNumbers = phoneNumberRepository.countByActiveTrue();
        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.countByStatus(SessionStatus.ACTIVE);
        long callsToday = callLogRepository.countCallsSince(todayStart);
        long completedCallsToday = callLogRepository.countCompletedCallsSince(todayStart);
        double successRate = callsToday > 0 ? (double) completedCallsToday / callsToday * 100 : 0;

        DashboardStats stats = DashboardStats.builder()
                .totalPhoneNumbers(totalPhoneNumbers)
                .activePhoneNumbers(activePhoneNumbers)
                .totalSessions(totalSessions)
                .activeSessions(activeSessions)
                .callsToday(callsToday)
                .completedCallsToday(completedCallsToday)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .build();

        return ResponseEntity.ok(stats);
    }
}
