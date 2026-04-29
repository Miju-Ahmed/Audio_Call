package com.audiocall.telephony;

import com.audiocall.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.conference.Participant;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class TwilioProvider implements TelephonyProvider {

    private final TwilioConfig twilioConfig;

    @Value("${app.webhook-base-url}")
    private String webhookBaseUrl;

    @Override
    public String initiateCall(String toNumber, String conferenceName,
                                String statusCallbackUrl, boolean muted) {
        try {
            String voiceUrl = webhookBaseUrl + "/webhook/voice?conference=" + conferenceName
                    + "&muted=" + muted;

            Call call = Call.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    URI.create(voiceUrl)
            )
            .setStatusCallback(URI.create(statusCallbackUrl))
            .setStatusCallbackEvent(Arrays.asList("initiated", "ringing", "answered", "completed"))
            .setStatusCallbackMethod(com.twilio.http.HttpMethod.POST)
            .setMethod(com.twilio.http.HttpMethod.POST)
            .setTimeout(30)
            .create();

            log.info("Initiated call to {} - SID: {}", toNumber, call.getSid());
            return call.getSid();

        } catch (Exception e) {
            log.error("Failed to initiate call to {}: {}", toNumber, e.getMessage());
            throw new RuntimeException("Failed to initiate call to " + toNumber, e);
        }
    }

    @Override
    public void endCall(String callSid) {
        try {
            Call.updater(callSid)
                .setStatus(Call.UpdateStatus.COMPLETED)
                .update();
            log.info("Ended call: {}", callSid);
        } catch (Exception e) {
            log.error("Failed to end call {}: {}", callSid, e.getMessage());
        }
    }

    @Override
    public void muteParticipant(String conferenceSid, String callSid, boolean muted) {
        try {
            Participant.updater(conferenceSid, callSid)
                .setMuted(muted)
                .update();
            log.info("Participant {} {} in conference {}",
                    callSid, muted ? "muted" : "unmuted", conferenceSid);
        } catch (Exception e) {
            log.error("Failed to mute/unmute participant {} in conference {}: {}",
                    callSid, conferenceSid, e.getMessage());
        }
    }

    @Override
    public void endConference(String conferenceSid) {
        try {
            com.twilio.rest.api.v2010.account.Conference
                .updater(conferenceSid)
                .setStatus(com.twilio.rest.api.v2010.account.Conference.UpdateStatus.COMPLETED)
                .update();
            log.info("Ended conference: {}", conferenceSid);
        } catch (Exception e) {
            log.error("Failed to end conference {}: {}", conferenceSid, e.getMessage());
        }
    }
}
