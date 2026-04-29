package com.audiocall.dto;

import com.audiocall.model.CallStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallStatusUpdate {
    private Long sessionId;
    private Long callLogId;
    private String phoneNumber;
    private String callSid;
    private CallStatus status;
    private String failureReason;
    private Integer durationSeconds;
    private int connected;
    private int failed;
    private int totalNumbers;
}
