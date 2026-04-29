package com.audiocall.controller;

import com.audiocall.dto.SessionRequest;
import com.audiocall.dto.SessionStats;
import com.audiocall.model.CallLog;
import com.audiocall.model.CallSession;
import com.audiocall.service.CallSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class CallSessionController {

    private final CallSessionService service;

    @GetMapping
    public ResponseEntity<List<CallSession>> getAll() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallSession> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSession(id));
    }

    @PostMapping
    public ResponseEntity<CallSession> create(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(service.createSession(request));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<CallSession> start(@PathVariable Long id) {
        return ResponseEntity.ok(service.startSession(id));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<CallSession> stop(@PathVariable Long id) {
        return ResponseEntity.ok(service.stopSession(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<SessionStats> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSessionStats(id));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<CallLog>> getLogs(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSessionCallLogs(id));
    }

    @PostMapping("/{id}/mute/{callSid}")
    public ResponseEntity<Map<String, String>> mute(
            @PathVariable Long id,
            @PathVariable String callSid) {
        service.muteParticipant(id, callSid, true);
        return ResponseEntity.ok(Map.of("status", "muted"));
    }

    @PostMapping("/{id}/unmute/{callSid}")
    public ResponseEntity<Map<String, String>> unmute(
            @PathVariable Long id,
            @PathVariable String callSid) {
        service.muteParticipant(id, callSid, false);
        return ResponseEntity.ok(Map.of("status", "unmuted"));
    }
}
