package com.audiocall.service;

import com.audiocall.dto.CallStatusUpdate;
import com.audiocall.dto.SessionRequest;
import com.audiocall.dto.SessionStats;
import com.audiocall.model.*;
import com.audiocall.repository.CallLogRepository;
import com.audiocall.repository.CallSessionRepository;
import com.audiocall.telephony.TelephonyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallSessionService {

    private final CallSessionRepository sessionRepository;
    private final CallLogRepository callLogRepository;
    private final PhoneNumberService phoneNumberService;
    private final TelephonyProvider telephonyProvider;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.webhook-base-url}")
    private String webhookBaseUrl;

    @Value("${app.call.batch-size}")
    private int batchSize;

    @Value("${app.call.batch-delay-ms}")
    private long batchDelayMs;

    // ───── Session CRUD ─────

    public List<CallSession> getAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public CallSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }

    @Transactional
    public CallSession createSession(SessionRequest request) {
        // Determine which phone numbers to call
        List<PhoneNumber> numbers;
        if (request.getPhoneNumberIds() != null && !request.getPhoneNumberIds().isEmpty()) {
            numbers = phoneNumberService.getNumbersByIds(request.getPhoneNumberIds());
        } else if (request.getGroup() != null && !request.getGroup().isEmpty()) {
            numbers = phoneNumberService.getActiveNumbersByGroup(request.getGroup());
        } else {
            numbers = phoneNumberService.getActiveNumbers();
        }

        if (numbers.isEmpty()) {
            throw new RuntimeException("No phone numbers available for this session");
        }

        String conferenceName = "session-" + UUID.randomUUID().toString().substring(0, 8);

        CallSession session = CallSession.builder()
                .title(request.getTitle())
                .mode(request.getMode())
                .status(SessionStatus.CREATED)
                .conferenceName(conferenceName)
                .totalNumbers(numbers.size())
                .build();

        session = sessionRepository.save(session);

        // Create call log entries for each number
        for (PhoneNumber phone : numbers) {
            CallLog callLog = CallLog.builder()
                    .session(session)
                    .phoneNumber(phone.getNumber())
                    .status(CallStatus.QUEUED)
                    .retryCount(0)
                    .muted(request.getMode() == SessionMode.BROADCAST)
                    .build();
            callLogRepository.save(callLog);
        }

        log.info("Created session '{}' with {} numbers, conference: {}",
                session.getTitle(), numbers.size(), conferenceName);

        return session;
    }

    // ───── Session Control ─────

    @Transactional
    public CallSession startSession(Long sessionId) {
        CallSession session = getSession(sessionId);

        if (session.getStatus() != SessionStatus.CREATED) {
            throw new RuntimeException("Session can only be started from CREATED status. Current: " + session.getStatus());
        }

        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        // Start batch calling asynchronously
        startBatchCalling(sessionId);

        log.info("Started session: {} ({})", session.getTitle(), sessionId);
        return session;
    }

    @Transactional
    public CallSession stopSession(Long sessionId) {
        CallSession session = getSession(sessionId);

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new RuntimeException("Session is not active. Current: " + session.getStatus());
        }

        // End all active calls
        List<CallLog> activeCalls = callLogRepository.findBySessionIdAndStatus(sessionId, CallStatus.IN_PROGRESS);
        for (CallLog cl : activeCalls) {
            try {
                telephonyProvider.endCall(cl.getCallSid());
                cl.setStatus(CallStatus.CANCELLED);
                cl.setEndedAt(LocalDateTime.now());
                callLogRepository.save(cl);
            } catch (Exception e) {
                log.error("Failed to end call {}: {}", cl.getCallSid(), e.getMessage());
            }
        }

        // Cancel queued calls
        List<CallLog> queuedCalls = callLogRepository.findBySessionIdAndStatus(sessionId, CallStatus.QUEUED);
        for (CallLog cl : queuedCalls) {
            cl.setStatus(CallStatus.CANCELLED);
            callLogRepository.save(cl);
        }

        // End conference if it exists
        if (session.getConferenceSid() != null) {
            telephonyProvider.endConference(session.getConferenceSid());
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        broadcastSessionUpdate(session);
        log.info("Stopped session: {} ({})", session.getTitle(), sessionId);
        return session;
    }

    // ───── Batch Calling Engine ─────

    @Async("callExecutor")
    public void startBatchCalling(Long sessionId) {
        try {
            List<CallLog> queuedCalls = callLogRepository.findBySessionIdAndStatus(sessionId, CallStatus.QUEUED);
            String statusCallbackUrl = webhookBaseUrl + "/webhook/status?sessionId=" + sessionId;

            CallSession session = getSession(sessionId);

            log.info("Starting batch calling for session {} - {} calls to make", sessionId, queuedCalls.size());

            // Process in batches
            for (int i = 0; i < queuedCalls.size(); i += batchSize) {
                // Check if session is still active
                CallSession currentSession = getSession(sessionId);
                if (currentSession.getStatus() != SessionStatus.ACTIVE) {
                    log.info("Session {} no longer active, stopping batch calling", sessionId);
                    break;
                }

                int end = Math.min(i + batchSize, queuedCalls.size());
                List<CallLog> batch = queuedCalls.subList(i, end);

                log.info("Processing batch {}/{} ({} calls)",
                        (i / batchSize) + 1,
                        (int) Math.ceil((double) queuedCalls.size() / batchSize),
                        batch.size());

                // Initiate calls in this batch concurrently
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (CallLog callLog : batch) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        initiateCall(callLog, session.getConferenceName(), statusCallbackUrl);
                    }));
                }

                // Wait for all calls in this batch to be initiated
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Delay before next batch
                if (end < queuedCalls.size()) {
                    Thread.sleep(batchDelayMs);
                }
            }

            log.info("Batch calling completed for session {}", sessionId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Batch calling interrupted for session {}", sessionId);
        } catch (Exception e) {
            log.error("Error in batch calling for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    private void initiateCall(CallLog callLog, String conferenceName, String statusCallbackUrl) {
        try {
            callLog.setInitiatedAt(LocalDateTime.now());
            callLog.setStatus(CallStatus.RINGING);
            callLogRepository.save(callLog);

            String callSid = telephonyProvider.initiateCall(
                    callLog.getPhoneNumber(),
                    conferenceName,
                    statusCallbackUrl,
                    callLog.isMuted()
            );

            callLog.setCallSid(callSid);
            callLogRepository.save(callLog);

            broadcastCallUpdate(callLog);

        } catch (Exception e) {
            callLog.setStatus(CallStatus.FAILED);
            callLog.setFailureReason(e.getMessage());
            callLogRepository.save(callLog);

            updateSessionCounts(callLog.getSession().getId());
            broadcastCallUpdate(callLog);

            log.error("Failed to initiate call to {}: {}", callLog.getPhoneNumber(), e.getMessage());
        }
    }

    // ───── Call Status Handling ─────

    @Transactional
    public void handleCallStatusUpdate(String callSid, String twilioStatus, Integer duration,
                                        Long sessionId) {
        Optional<CallLog> optCallLog = callLogRepository.findByCallSid(callSid);
        if (optCallLog.isEmpty()) {
            log.warn("Received status update for unknown call SID: {}", callSid);
            return;
        }

        CallLog callLog = optCallLog.get();
        CallStatus newStatus = mapTwilioStatus(twilioStatus);
        callLog.setStatus(newStatus);

        if (newStatus == CallStatus.IN_PROGRESS && callLog.getAnsweredAt() == null) {
            callLog.setAnsweredAt(LocalDateTime.now());
        }

        if (newStatus == CallStatus.COMPLETED || newStatus == CallStatus.FAILED
                || newStatus == CallStatus.BUSY || newStatus == CallStatus.NO_ANSWER) {
            callLog.setEndedAt(LocalDateTime.now());
            if (duration != null) {
                callLog.setDurationSeconds(duration);
            }
        }

        callLogRepository.save(callLog);
        updateSessionCounts(callLog.getSession().getId());
        broadcastCallUpdate(callLog);

        log.debug("Call {} status updated to {} (Twilio: {})", callSid, newStatus, twilioStatus);
    }

    @Transactional
    public void handleConferenceStatusUpdate(String conferenceSid, String conferenceName,
                                              String event) {
        Optional<CallSession> optSession = sessionRepository.findByConferenceName(conferenceName);
        if (optSession.isPresent()) {
            CallSession session = optSession.get();
            if (session.getConferenceSid() == null) {
                session.setConferenceSid(conferenceSid);
                sessionRepository.save(session);
            }
            log.info("Conference event: {} for session {}", event, session.getTitle());
        }
    }

    // ───── Participant Management ─────

    public void muteParticipant(Long sessionId, String callSid, boolean muted) {
        CallSession session = getSession(sessionId);
        if (session.getConferenceSid() == null) {
            throw new RuntimeException("Conference not yet established for session " + sessionId);
        }
        telephonyProvider.muteParticipant(session.getConferenceSid(), callSid, muted);

        // Update call log
        callLogRepository.findByCallSid(callSid).ifPresent(cl -> {
            cl.setMuted(muted);
            callLogRepository.save(cl);
            broadcastCallUpdate(cl);
        });
    }

    // ───── Statistics ─────

    public SessionStats getSessionStats(Long sessionId) {
        CallSession session = getSession(sessionId);

        long queued = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.QUEUED);
        long ringing = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.RINGING);
        long connected = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.IN_PROGRESS);
        long completed = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.COMPLETED);
        long failed = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.FAILED);
        long busy = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.BUSY);
        long noAnswer = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.NO_ANSWER);

        long durationSeconds = 0;
        if (session.getStartedAt() != null) {
            LocalDateTime endTime = session.getEndedAt() != null ? session.getEndedAt() : LocalDateTime.now();
            durationSeconds = Duration.between(session.getStartedAt(), endTime).getSeconds();
        }

        return SessionStats.builder()
                .sessionId(sessionId)
                .title(session.getTitle())
                .mode(session.getMode())
                .status(session.getStatus())
                .totalNumbers(session.getTotalNumbers())
                .queued((int) queued)
                .ringing((int) ringing)
                .connected((int) connected)
                .completed((int) completed)
                .failed((int) failed)
                .busy((int) busy)
                .noAnswer((int) noAnswer)
                .startedAt(session.getStartedAt())
                .durationSeconds(durationSeconds)
                .build();
    }

    public List<CallLog> getSessionCallLogs(Long sessionId) {
        return callLogRepository.findBySessionId(sessionId);
    }

    // ───── Helper Methods ─────

    private void updateSessionCounts(Long sessionId) {
        CallSession session = getSession(sessionId);
        long connected = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.IN_PROGRESS);
        long failed = callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.FAILED)
                + callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.BUSY)
                + callLogRepository.countBySessionIdAndStatus(sessionId, CallStatus.NO_ANSWER);

        session.setConnected((int) connected);
        session.setFailed((int) failed);
        sessionRepository.save(session);
    }

    private CallStatus mapTwilioStatus(String twilioStatus) {
        if (twilioStatus == null) return CallStatus.QUEUED;
        return switch (twilioStatus.toLowerCase()) {
            case "queued" -> CallStatus.QUEUED;
            case "ringing" -> CallStatus.RINGING;
            case "in-progress" -> CallStatus.IN_PROGRESS;
            case "completed" -> CallStatus.COMPLETED;
            case "busy" -> CallStatus.BUSY;
            case "failed" -> CallStatus.FAILED;
            case "no-answer" -> CallStatus.NO_ANSWER;
            case "canceled" -> CallStatus.CANCELLED;
            default -> {
                log.warn("Unknown Twilio status: {}", twilioStatus);
                yield CallStatus.FAILED;
            }
        };
    }

    private void broadcastCallUpdate(CallLog callLog) {
        CallSession session = callLog.getSession();
        CallStatusUpdate update = CallStatusUpdate.builder()
                .sessionId(session.getId())
                .callLogId(callLog.getId())
                .phoneNumber(callLog.getPhoneNumber())
                .callSid(callLog.getCallSid())
                .status(callLog.getStatus())
                .failureReason(callLog.getFailureReason())
                .durationSeconds(callLog.getDurationSeconds())
                .connected(session.getConnected())
                .failed(session.getFailed())
                .totalNumbers(session.getTotalNumbers())
                .build();

        messagingTemplate.convertAndSend("/topic/call-updates", update);
    }

    private void broadcastSessionUpdate(CallSession session) {
        SessionStats stats = getSessionStats(session.getId());
        messagingTemplate.convertAndSend("/topic/session-stats", stats);
    }
}
