package com.volunteergrid.dto;

import com.volunteergrid.entity.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO for creating or editing an Event.
 */
@Data
public class EventDto {

    private Long id;

    @NotBlank(message = "Event title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Venue is required")
    @Size(max = 200)
    private String venue;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private EventStatus status;

    // Display fields (set during read, not form submission)
    private String organizerName;
    private int shiftCount;
    private long registeredVolunteers;
}
