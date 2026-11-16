package com.volunteergrid.service;

import com.volunteergrid.entity.*;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.ResourceNotFoundException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Manages check-in and check-out attendance recording.
 *
 * Business rules enforced:
 * - Only organizers of the event can mark attendance.
 * - Check-out must occur after check-in.
 * - Cannot check out without checking in first.
 * - Completed hours are derived automatically.
 */
@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AssignmentService assignmentService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             AssignmentService assignmentService) {
        this.attendanceRepository = attendanceRepository;
        this.assignmentService = assignmentService;
    }

    /**
     * Record check-in for an assignment.
     * Creates an Attendance record if one doesn't exist.
     */
    @Transactional
    public void checkIn(Long assignmentId, User organizer) {
        Assignment assignment = assignmentService.getAssignmentEntityById(assignmentId);
        assertIsOrganizerOfEvent(assignment.getShift().getEvent(), organizer);

        if (assignment.getStatus() != AssignmentStatus.APPROVED
                && assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessRuleException(
                    "Can only check in APPROVED or PENDING assignments.");
        }

        Attendance attendance = attendanceRepository
                .findByAssignmentId(assignmentId)
                .orElse(Attendance.builder().assignment(assignment).build());

        if (attendance.getCheckInTime() != null) {
            throw new BusinessRuleException("Volunteer has already checked in.");
        }

        attendance.setCheckInTime(LocalDateTime.now());
        attendanceRepository.save(attendance);

        // Upgrade assignment status to APPROVED if it was PENDING
        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            assignmentService.approve(assignmentId, organizer);
        }
    }

    /**
     * Record check-out for an assignment.
     * Recalculates completed hours and marks assignment as COMPLETED.
     */
    @Transactional
    public void checkOut(Long assignmentId, User organizer) {
        Assignment assignment = assignmentService.getAssignmentEntityById(assignmentId);
        assertIsOrganizerOfEvent(assignment.getShift().getEvent(), organizer);

        Attendance attendance = attendanceRepository
                .findByAssignmentId(assignmentId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Cannot check out before checking in."));

        if (attendance.getCheckInTime() == null) {
            throw new BusinessRuleException(
                    "Cannot check out before checking in.");
        }

        if (attendance.getCheckOutTime() != null) {
            throw new BusinessRuleException("Volunteer has already checked out.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(attendance.getCheckInTime())) {
            throw new BusinessRuleException(
                    "Check-out time must be after check-in time.");
        }

        attendance.setCheckOutTime(now);
        attendance.recalculateHours();
        attendanceRepository.save(attendance);

        // Mark assignment as COMPLETED
        assignmentService.markCompleted(assignmentId);
    }

    public Attendance findByAssignmentId(Long assignmentId) {
        return attendanceRepository.findByAssignmentId(assignmentId)
                .orElse(null);
    }

    public Double getTotalHoursForVolunteer(Long volunteerId) {
        return attendanceRepository.sumCompletedHoursByVolunteerId(volunteerId);
    }

    // ---- Private helpers ----

    private void assertIsOrganizerOfEvent(Event event, User user) {
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException(
                    "Only the event organizer can mark attendance.");
        }
    }
}
