package com.volunteergrid.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO for creating or viewing a Shift.
 */
@Data
public class ShiftDto {

    private Long id;

    private Long eventId;
    private String eventTitle;

    @NotBlank(message = "Task name is required")
    @Size(max = 200)
    private String taskName;

    @NotBlank(message = "Venue is required")
    @Size(max = 200)
    private String venue;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    // Computed: how many slots are already filled
    private long registeredCount;

    // Computed: remaining slots
    public int getRemainingSlots() {
        return capacity - (int) registeredCount;
    }

    public boolean isFull() {
        return registeredCount >= capacity;
    }
}
