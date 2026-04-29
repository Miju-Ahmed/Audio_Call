package com.audiocall.service;

import com.audiocall.model.CallLog;
import com.audiocall.model.CallSession;
import com.audiocall.model.CallStatus;
import com.audiocall.model.SessionStatus;
import com.audiocall.repository.CallLogRepository;
import com.audiocall.repository.CallSessionRepository;
import com.audiocall.telephony.TelephonyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallRetryService {

    private final CallLogRepository callLogRepository;
    private final CallSessionRepository sessionRepository;
    private final TelephonyProvider telephonyProvider;

    @Value("${app.call.retry-max}")
    private int maxRetries;

    @Value("${app.webhook-base-url}")
    private String webhookBaseUrl;

    private static final List<CallStatus> RETRYABLE_STATUSES = Arrays.asList(
            CallStatus.BUSY,
            CallStatus.FAILED,
            CallStatus.NO_ANSWER
    );

    /**
     * Scheduled task that runs every 60 seconds to retry failed calls
     * for all active sessions.
     */
    @Scheduled(fixedDelayString = "${app.call.retry-delay-seconds}000")
    public void retryFailedCalls() {
        List<CallSession> activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE);

        for (CallSession session : activeSessions) {
            List<CallLog> retryableCalls = callLogRepository.findRetryableCalls(
                    session.getId(), RETRYABLE_STATUSES, maxRetries);

            if (retryableCalls.isEmpty()) continue;

            log.info("Retrying {} calls for session '{}'", retryableCalls.size(), session.getTitle());

            String statusCallbackUrl = webhookBaseUrl + "/webhook/status?sessionId=" + session.getId();

            for (CallLog callLog : retryableCalls) {
                retryCall(callLog, session.getConferenceName(), statusCallbackUrl);
            }
        }
    }

    private void retryCall(CallLog callLog, String conferenceName, String statusCallbackUrl) {
        try {
            callLog.setRetryCount(callLog.getRetryCount() + 1);
            callLog.setStatus(CallStatus.RINGING);
            callLog.setInitiatedAt(LocalDateTime.now());
            callLog.setFailureReason(null);
            callLog.setAnsweredAt(null);
            callLog.setEndedAt(null);
            callLog.setDurationSeconds(null);

            String newCallSid = telephonyProvider.initiateCall(
                    callLog.getPhoneNumber(),
                    conferenceName,
                    statusCallbackUrl,
                    callLog.isMuted()
            );

            callLog.setCallSid(newCallSid);
            callLogRepository.save(callLog);

            log.info("Retry #{} for {} - new SID: {}",
                    callLog.getRetryCount(), callLog.getPhoneNumber(), newCallSid);

        } catch (Exception e) {
            callLog.setStatus(CallStatus.FAILED);
            callLog.setFailureReason("Retry failed: " + e.getMessage());
            callLogRepository.save(callLog);

            log.error("Retry #{} failed for {}: {}",
                    callLog.getRetryCount(), callLog.getPhoneNumber(), e.getMessage());
        }
    }
}
