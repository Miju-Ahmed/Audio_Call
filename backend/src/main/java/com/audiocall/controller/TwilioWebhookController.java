package com.audiocall.controller;

import com.audiocall.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles Twilio webhook callbacks for voice calls and conference events.
 * These endpoints must be publicly accessible (use ngrok in development).
 */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class TwilioWebhookController {

    private final CallSessionService callSessionService;

    /**
     * Called by Twilio when an outbound call is answered.
     * Returns TwiML instructions to connect the caller to the conference.
     */
    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleVoice(
            @RequestParam(value = "conference", required = false) String conferenceName,
            @RequestParam(value = "muted", defaultValue = "true") boolean muted) {

        log.info("Voice webhook: conference={}, muted={}", conferenceName, muted);

        if (conferenceName == null || conferenceName.isEmpty()) {
            return """
                <Response>
                    <Say>Sorry, this call session is no longer available. Goodbye.</Say>
                    <Hangup/>
                </Response>
                """;
        }

        // Build TwiML to connect to conference
        String mutedAttr = muted ? "true" : "false";
        String startOnEnter = muted ? "false" : "true";

        return String.format("""
            <Response>
                <Say>You are now connected to the broadcast. Please wait.</Say>
                <Dial>
                    <Conference muted="%s" beep="false" startConferenceOnEnter="%s"
                                endConferenceOnExit="false" waitUrl="">
                        %s
                    </Conference>
                </Dial>
            </Response>
            """, mutedAttr, startOnEnter, conferenceName);
    }

    /**
     * Called by Twilio with call status updates (initiated, ringing, answered, completed, etc.)
     */
    @PostMapping("/status")
    public String handleStatusCallback(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "sessionId", required = false) Long sessionId) {

        String callSid = params.get("CallSid");
        String callStatus = params.get("CallStatus");
        String durationStr = params.get("CallDuration");
        Integer duration = durationStr != null ? Integer.parseInt(durationStr) : null;

        log.info("Status callback: SID={}, status={}, duration={}, sessionId={}",
                callSid, callStatus, duration, sessionId);

        if (callSid != null && callStatus != null) {
            callSessionService.handleCallStatusUpdate(callSid, callStatus, duration, sessionId);
        }

        return "<Response/>";
    }

    /**
     * Called by Twilio with conference events (participant-join, participant-leave, etc.)
     */
    @PostMapping("/conference-status")
    public String handleConferenceStatus(@RequestParam Map<String, String> params) {
        String conferenceSid = params.get("ConferenceSid");
        String friendlyName = params.get("FriendlyName");
        String statusEvent = params.get("StatusCallbackEvent");

        log.info("Conference event: SID={}, name={}, event={}",
                conferenceSid, friendlyName, statusEvent);

        if (conferenceSid != null && friendlyName != null) {
            callSessionService.handleConferenceStatusUpdate(conferenceSid, friendlyName, statusEvent);
        }

        return "<Response/>";
    }
}
