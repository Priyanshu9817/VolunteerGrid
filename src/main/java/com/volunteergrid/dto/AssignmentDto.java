package com.volunteergrid.dto;

import com.volunteergrid.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for displaying an Assignment with all related details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

    private Long id;

    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;

    private Long shiftId;
    private String shiftTaskName;
    private String shiftVenue;
    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;

    private Long eventId;
    private String eventTitle;

    private AssignmentStatus status;
    private LocalDateTime registrationTime;

    // Attendance info (if available)
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Double completedHours;
}
