package com.volunteergrid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Volunteer contribution summary shown on the volunteer's dashboard.
 * All hours come from verified Attendance records only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerSummaryDto {

    private Long volunteerId;
    private String volunteerName;

    private long totalEventsParticipated;   // distinct events with approved assignment
    private long totalShiftsAssigned;       // all approved/completed assignments
    private long completedShifts;           // assignments with status=COMPLETED
    private double totalVerifiedHours;      // sum from Attendance.completedHours

    private long pendingAssignments;        // awaiting organizer approval
    private long approvedAssignments;       // approved but not yet attended
}
