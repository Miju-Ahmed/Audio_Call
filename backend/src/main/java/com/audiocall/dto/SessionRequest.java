package com.audiocall.dto;

import com.audiocall.model.SessionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRequest {

    @NotBlank(message = "Session title is required")
    private String title;

    @NotNull(message = "Session mode is required")
    private SessionMode mode;

    /** Optional: specific phone number IDs. If empty, all active numbers are used. */
    private List<Long> phoneNumberIds;

    /** Optional: filter by group name */
    private String group;
}
