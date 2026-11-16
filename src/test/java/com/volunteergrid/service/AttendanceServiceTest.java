package com.volunteergrid.service;

import com.volunteergrid.entity.*;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AttendanceService.
 * Validates check-in/out workflows, organizer authorization, and completed hours calculation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService Tests")
public class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private AttendanceService attendanceService;

    private User organizer;
    private User otherUser;
    private User volunteer;
    private Event event;
    private Shift shift;
    private Assignment approvedAssignment;
    private Assignment pendingAssignment;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).name("Organizer").email("org@test.com")
                .role(Role.ORGANIZER).build();
        otherUser = User.builder().id(9L).name("Other").email("other@test.com")
                .role(Role.ORGANIZER).build();
        volunteer = User.builder().id(2L).name("Volunteer").email("vol@test.com")
                .role(Role.VOLUNTEER).build();

        event = Event.builder()
                .id(10L).title("Tech Fest").organizer(organizer)
                .status(EventStatus.OPEN).build();

        shift = Shift.builder()
                .id(100L).taskName("Helpdesk").event(event).capacity(10)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .build();

        approvedAssignment = Assignment.builder()
                .id(501L).shift(shift).volunteer(volunteer)
                .status(AssignmentStatus.APPROVED).build();

        pendingAssignment = Assignment.builder()
                .id(502L).shift(shift).volunteer(volunteer)
                .status(AssignmentStatus.PENDING).build();
    }

    @Test
    @DisplayName("Should successfully record check-in for APPROVED assignment")
    void checkIn_approvedAssignment_succeeds() {
        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);
        when(attendanceRepository.findByAssignmentId(501L)).thenReturn(Optional.empty());

        assertThatCode(() -> attendanceService.checkIn(501L, organizer))
                .doesNotThrowAnyException();

        verify(attendanceRepository, times(1)).save(argThat(a -> a.getCheckInTime() != null));
    }

    @Test
    @DisplayName("Should approve PENDING assignment upon check-in")
    void checkIn_pendingAssignment_approvesAndChecksIn() {
        when(assignmentService.getAssignmentEntityById(502L)).thenReturn(pendingAssignment);
        when(attendanceRepository.findByAssignmentId(502L)).thenReturn(Optional.empty());

        attendanceService.checkIn(502L, organizer);

        verify(assignmentService, times(1)).approve(502L, organizer);
        verify(attendanceRepository, times(1)).save(argThat(a -> a.getCheckInTime() != null));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException on duplicate check-in")
    void checkIn_alreadyCheckedIn_throwsException() {
        Attendance existing = Attendance.builder()
                .assignment(approvedAssignment)
                .checkInTime(LocalDateTime.now().minusHours(1))
                .build();

        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);
        when(attendanceRepository.findByAssignmentId(501L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> attendanceService.checkIn(501L, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already checked in");
    }

    @Test
    @DisplayName("Should throw UnauthorizedActionException when non-organizer marks attendance")
    void checkIn_nonOrganizer_throwsUnauthorized() {
        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);

        assertThatThrownBy(() -> attendanceService.checkIn(501L, otherUser))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("Should successfully record check-out, calculate hours, and complete assignment")
    void checkOut_validCheckIn_completesAssignment() {
        LocalDateTime checkIn = LocalDateTime.now().minusHours(3);
        Attendance attendance = Attendance.builder()
                .assignment(approvedAssignment)
                .checkInTime(checkIn)
                .build();

        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);
        when(attendanceRepository.findByAssignmentId(501L)).thenReturn(Optional.of(attendance));

        attendanceService.checkOut(501L, organizer);

        assertThat(attendance.getCheckOutTime()).isNotNull();
        assertThat(attendance.getCompletedHours()).isGreaterThan(0.0);
        verify(attendanceRepository).save(attendance);
        verify(assignmentService).markCompleted(501L);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when checking out without check-in")
    void checkOut_withoutCheckIn_throwsException() {
        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);
        when(attendanceRepository.findByAssignmentId(501L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkOut(501L, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot check out before checking in");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when checking out a second time")
    void checkOut_alreadyCheckedOut_throwsException() {
        Attendance attendance = Attendance.builder()
                .assignment(approvedAssignment)
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .build();

        when(assignmentService.getAssignmentEntityById(501L)).thenReturn(approvedAssignment);
        when(attendanceRepository.findByAssignmentId(501L)).thenReturn(Optional.of(attendance));

        assertThatThrownBy(() -> attendanceService.checkOut(501L, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already checked out");
    }
}
