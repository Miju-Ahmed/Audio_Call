package com.audiocall.telephony;

/**
 * Abstraction layer for telephony providers.
 * Implementations can use Twilio, Asterisk, Plivo, etc.
 */
public interface TelephonyProvider {

    /**
     * Initiate an outbound call to the given number and connect them to a conference.
     *
     * @param toNumber         The destination phone number (E.164 format)
     * @param conferenceName   The unique conference room name
     * @param statusCallbackUrl URL for call status webhook callbacks
     * @param muted            Whether the participant should be muted upon joining
     * @return The call SID / identifier from the telephony provider
     */
    String initiateCall(String toNumber, String conferenceName, String statusCallbackUrl, boolean muted);

    /**
     * End/hang up an active call.
     *
     * @param callSid The call SID to terminate
     */
    void endCall(String callSid);

    /**
     * Mute or unmute a conference participant.
     *
     * @param conferenceSid The conference SID
     * @param callSid       The participant's call SID
     * @param muted         true to mute, false to unmute
     */
    void muteParticipant(String conferenceSid, String callSid, boolean muted);

    /**
     * End an entire conference, disconnecting all participants.
     *
     * @param conferenceSid The conference SID
     */
    void endConference(String conferenceSid);
}
