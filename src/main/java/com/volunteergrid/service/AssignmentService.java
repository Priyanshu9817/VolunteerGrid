package com.volunteergrid.service;

import com.volunteergrid.dto.AssignmentDto;
import com.volunteergrid.entity.*;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.ResourceNotFoundException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.AssignmentRepository;
import com.volunteergrid.repository.AttendanceRepository;
import com.volunteergrid.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core business service for volunteer assignment management.
 *
 * Enforced rules:
 * 1. Event must be OPEN to accept new registrations.
 * 2. Shift must have remaining capacity.
 * 3. No duplicate registrations (same volunteer + same shift).
 * 4. No overlapping active shifts for the same volunteer (conflict detection).
 * 5. Only approved assignments can be marked as attended/completed.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShiftService shiftService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             ShiftRepository shiftRepository,
                             AttendanceRepository attendanceRepository,
                             ShiftService shiftService) {
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
        this.attendanceRepository = attendanceRepository;
        this.shiftService = shiftService;
    }

    /**
     * Register a volunteer for a shift.
     * All business-rule checks run inside a single transaction.
     */
    @Transactional
    public AssignmentDto register(Long shiftId, User volunteer) {
        Shift shift = shiftService.getShiftEntityById(shiftId);
        Event event = shift.getEvent();

        // Rule 1: Event must be OPEN
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessRuleException(
                    "Registrations are not accepted for event '" + event.getTitle()
                            + "' (status: " + event.getStatus() + ").");
        }

        // Rule 2: No duplicate registration
        if (assignmentRepository.existsByVolunteerIdAndShiftId(
                volunteer.getId(), shiftId)) {
            throw new BusinessRuleException(
                    "You have already registered for this shift.");
        }

        // Rule 3: Capacity check
        long activeCount = assignmentRepository.countActiveByShiftId(shiftId);
        if (activeCount >= shift.getCapacity()) {
            throw new BusinessRuleException(
                    "This shift is full (capacity: " + shift.getCapacity() + ").");
        }

        // Rule 4: Conflict check — no overlapping shifts
        List<Shift> conflicts = shiftRepository.findConflictingShiftsForVolunteer(
                volunteer.getId(), shift.getStartTime(), shift.getEndTime());
        // Exclude the shift being registered (shouldn't be active yet, but defensive)
        List<Shift> realConflicts = conflicts.stream()
                .filter(s -> !s.getId().equals(shiftId))
                .toList();
        if (!realConflicts.isEmpty()) {
            String conflictDesc = realConflicts.get(0).getTaskName()
                    + " (" + realConflicts.get(0).getStartTime()
                    + " – " + realConflicts.get(0).getEndTime() + ")";
            throw new BusinessRuleException(
                    "This shift overlaps with your existing assignment: " + conflictDesc);
        }

        Assignment assignment = Assignment.builder()
                .volunteer(volunteer)
                .shift(shift)
                .status(AssignmentStatus.PENDING)
                .registrationTime(LocalDateTime.now())
                .build();

        return toDto(assignmentRepository.save(assignment));
    }

    /**
     * Volunteer withdraws from a shift.
     */
    @Transactional
    public void withdraw(Long assignmentId, User requestingUser) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);

        if (!assignment.getVolunteer().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedActionException(
                    "You can only withdraw your own registrations.");
        }

        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Cannot withdraw from a completed assignment.");
        }

        assignment.setStatus(AssignmentStatus.WITHDRAWN);
        assignmentRepository.save(assignment);
    }

    /**
     * Organizer approves a pending registration.
     */
    @Transactional
    public void approve(Long assignmentId, User organizer) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertIsOrganizerOfEvent(assignment.getShift().getEvent(), organizer);

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING assignments can be approved.");
        }

        assignment.setStatus(AssignmentStatus.APPROVED);
        assignmentRepository.save(assignment);
    }

    /**
     * Organizer rejects a pending registration.
     */
    @Transactional
    public void reject(Long assignmentId, User organizer) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertIsOrganizerOfEvent(assignment.getShift().getEvent(), organizer);

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING assignments can be rejected.");
        }

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignmentRepository.save(assignment);
    }

    /**
     * Mark assignment as COMPLETED (called after checkout).
     */
    @Transactional
    public void markCompleted(Long assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(assignment);
    }

    public AssignmentDto findById(Long id) {
        return toDto(getAssignmentOrThrow(id));
    }

    public Assignment getAssignmentEntityById(Long id) {
        return getAssignmentOrThrow(id);
    }

    public List<AssignmentDto> findByVolunteer(Long volunteerId) {
        return assignmentRepository.findByVolunteerId(volunteerId)
                .stream().map(this::toDto).toList();
    }

    public List<AssignmentDto> findByShift(Long shiftId) {
        return assignmentRepository.findByShiftId(shiftId)
                .stream().map(this::toDto).toList();
    }

    public List<AssignmentDto> findByEvent(Long eventId) {
        return assignmentRepository.findByEventId(eventId)
                .stream().map(this::toDto).toList();
    }

    // ---- Private helpers ----

    private Assignment getAssignmentOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + id));
    }

    private void assertIsOrganizerOfEvent(Event event, User user) {
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException(
                    "You are not the organizer of this event.");
        }
    }

    public AssignmentDto toDto(Assignment a) {
        Shift shift = a.getShift();
        Event event = shift.getEvent();
        Attendance att = a.getAttendance();

        return AssignmentDto.builder()
                .id(a.getId())
                .volunteerId(a.getVolunteer().getId())
                .volunteerName(a.getVolunteer().getName())
                .volunteerEmail(a.getVolunteer().getEmail())
                .shiftId(shift.getId())
                .shiftTaskName(shift.getTaskName())
                .shiftVenue(shift.getVenue())
                .shiftStartTime(shift.getStartTime())
                .shiftEndTime(shift.getEndTime())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .status(a.getStatus())
                .registrationTime(a.getRegistrationTime())
                .checkInTime(att != null ? att.getCheckInTime() : null)
                .checkOutTime(att != null ? att.getCheckOutTime() : null)
                .completedHours(att != null ? att.getCompletedHours() : null)
                .build();
    }
}
