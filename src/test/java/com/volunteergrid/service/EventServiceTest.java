package com.volunteergrid.service;

import com.volunteergrid.dto.EventDto;
import com.volunteergrid.entity.*;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.EventRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventService.
 * Uses Mockito to mock dependencies — no Spring context or DB needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private User anotherUser;
    private EventDto validEventDto;
    private Event savedEvent;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(1L).name("Priya").email("priya@test.com")
                .role(Role.ORGANIZER).enabled(true).build();

        anotherUser = User.builder()
                .id(2L).name("Other").email("other@test.com")
                .role(Role.ORGANIZER).enabled(true).build();

        validEventDto = new EventDto();
        validEventDto.setTitle("Test Event");
        validEventDto.setVenue("Test Venue");
        validEventDto.setStartTime(LocalDateTime.now().plusDays(1));
        validEventDto.setEndTime(LocalDateTime.now().plusDays(2));
        validEventDto.setStatus(EventStatus.OPEN);

        savedEvent = Event.builder()
                .id(10L)
                .title("Test Event")
                .venue("Test Venue")
                .startTime(validEventDto.getStartTime())
                .endTime(validEventDto.getEndTime())
                .status(EventStatus.OPEN)
                .organizer(organizer)
                .build();
    }

    @Test
    @DisplayName("Should create event successfully with valid data")
    void createEvent_validData_returnsDto() {
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventDto result = eventService.createEvent(validEventDto, organizer);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Event");
        assertThat(result.getStatus()).isEqualTo(EventStatus.OPEN);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when end time is before start time")
    void createEvent_endBeforeStart_throwsException() {
        validEventDto.setEndTime(validEventDto.getStartTime().minusHours(1));

        assertThatThrownBy(() -> eventService.createEvent(validEventDto, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("end time must be after start time");
    }

    @Test
    @DisplayName("Should throw UnauthorizedActionException when non-owner tries to edit")
    void updateEvent_nonOwner_throwsUnauthorized() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(savedEvent));

        assertThatThrownBy(() -> eventService.updateEvent(10L, validEventDto, anotherUser))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("Admin can edit any event regardless of ownership")
    void updateEvent_adminUser_succeeds() {
        User admin = User.builder()
                .id(99L).name("Admin").email("admin@test.com")
                .role(Role.ADMIN).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(savedEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        assertThatCode(() -> eventService.updateEvent(10L, validEventDto, admin))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should change event status successfully")
    void changeStatus_validOrganizer_succeeds() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(savedEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        assertThatCode(() ->
                eventService.changeStatus(10L, EventStatus.CLOSED, organizer))
                .doesNotThrowAnyException();

        verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.CLOSED));
    }
}
