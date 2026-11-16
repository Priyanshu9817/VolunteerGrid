package com.volunteergrid.service;

import com.volunteergrid.dto.AssignmentDto;
import com.volunteergrid.entity.*;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.repository.AssignmentRepository;
import com.volunteergrid.repository.AttendanceRepository;
import com.volunteergrid.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssignmentService — the most business-critical service.
 * Tests all 4 registration rules: event open, no duplicate, capacity, no conflict.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Tests")
public class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private ShiftService shiftService;

    @InjectMocks
    private AssignmentService assignmentService;

    private User volunteer;
    private User organizer;
    private Event openEvent;
    private Event closedEvent;
    private Shift shift;
    private Shift closedShift;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).name("Org").email("org@t.com")
                .role(Role.ORGANIZER).build();
        volunteer = User.builder().id(2L).name("Vol").email("vol@t.com")
                .role(Role.VOLUNTEER).build();

        openEvent = Event.builder()
                .id(10L).title("Open Event")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .status(EventStatus.OPEN)
                .organizer(organizer).build();

        closedEvent = Event.builder()
                .id(11L).title("Closed Event")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .status(EventStatus.CLOSED)
                .organizer(organizer).build();

        shift = Shift.builder()
                .id(100L)
                .taskName("Registration Desk")
                .venue("Main Gate")
                .startTime(LocalDateTime.now().plusDays(1).withHour(9))
                .endTime(LocalDateTime.now().plusDays(1).withHour(12))
                .capacity(5)
                .event(openEvent)
                .build();

        closedShift = Shift.builder()
                .id(101L)
                .taskName("Closed Desk")
                .venue("Hall")
                .startTime(LocalDateTime.now().plusDays(1).withHour(9))
                .endTime(LocalDateTime.now().plusDays(1).withHour(12))
                .capacity(5)
                .event(closedEvent)
                .build();
    }

    @Test
    @DisplayName("Rule 1: Should throw when event is CLOSED")
    void register_closedEvent_throwsBusinessRuleException() {
        when(shiftService.getShiftEntityById(101L)).thenReturn(closedShift);

        assertThatThrownBy(() -> assignmentService.register(101L, volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not accepted");
    }

    @Test
    @DisplayName("Rule 2: Should throw on duplicate registration")
    void register_duplicate_throwsBusinessRuleException() {
        when(shiftService.getShiftEntityById(100L)).thenReturn(shift);
        when(assignmentRepository.existsByVolunteerIdAndShiftId(2L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.register(100L, volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Rule 3: Should throw when shift is at full capacity")
    void register_shiftFull_throwsBusinessRuleException() {
        when(shiftService.getShiftEntityById(100L)).thenReturn(shift);
        when(assignmentRepository.existsByVolunteerIdAndShiftId(2L, 100L)).thenReturn(false);
        // capacity = 5, active count = 5 (full)
        when(assignmentRepository.countActiveByShiftId(100L)).thenReturn(5L);

        assertThatThrownBy(() -> assignmentService.register(100L, volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("full");
    }

    @Test
    @DisplayName("Rule 4: Should throw on overlapping shift")
    void register_conflictingShift_throwsBusinessRuleException() {
        when(shiftService.getShiftEntityById(100L)).thenReturn(shift);
        when(assignmentRepository.existsByVolunteerIdAndShiftId(2L, 100L)).thenReturn(false);
        when(assignmentRepository.countActiveByShiftId(100L)).thenReturn(2L);

        // Return a conflicting shift
        Shift conflicting = Shift.builder()
                .id(999L)
                .taskName("Conflicting Task")
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .event(openEvent)
                .build();
        when(shiftRepository.findConflictingShiftsForVolunteer(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(conflicting));

        assertThatThrownBy(() -> assignmentService.register(100L, volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    @DisplayName("Should register successfully when all rules pass")
    void register_allRulesPassed_returnsDto() {
        when(shiftService.getShiftEntityById(100L)).thenReturn(shift);
        when(assignmentRepository.existsByVolunteerIdAndShiftId(2L, 100L)).thenReturn(false);
        when(assignmentRepository.countActiveByShiftId(100L)).thenReturn(2L);
        when(shiftRepository.findConflictingShiftsForVolunteer(
                anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Assignment saved = Assignment.builder()
                .id(500L).volunteer(volunteer).shift(shift)
                .status(AssignmentStatus.PENDING)
                .registrationTime(LocalDateTime.now()).build();
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(saved);

        AssignmentDto result = assignmentService.register(100L, volunteer);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AssignmentStatus.PENDING);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Organizer can approve PENDING assignment")
    void approve_pendingAssignment_statusChangesToApproved() {
        Assignment pending = Assignment.builder()
                .id(1L).volunteer(volunteer).shift(shift)
                .status(AssignmentStatus.PENDING).build();

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(assignmentRepository.save(any())).thenReturn(pending);

        assertThatCode(() -> assignmentService.approve(1L, organizer))
                .doesNotThrowAnyException();

        verify(assignmentRepository).save(argThat(
                a -> a.getStatus() == AssignmentStatus.APPROVED));
    }

    @Test
    @DisplayName("Cannot approve already-approved assignment")
    void approve_alreadyApproved_throwsException() {
        Assignment approved = Assignment.builder()
                .id(1L).volunteer(volunteer).shift(shift)
                .status(AssignmentStatus.APPROVED).build();

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> assignmentService.approve(1L, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING");
    }
}
