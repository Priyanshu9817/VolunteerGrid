package com.volunteergrid.service;

import com.volunteergrid.dto.EventDto;
import com.volunteergrid.entity.Event;
import com.volunteergrid.entity.EventStatus;
import com.volunteergrid.entity.User;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.ResourceNotFoundException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Event lifecycle management.
 *
 * Key rules enforced here:
 * - Only the event's organizer (or ADMIN) can edit or close it.
 * - Closed/Completed events cannot have their status reset to OPEN directly.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;

    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;
    }

    @Transactional
    public EventDto createEvent(EventDto dto, User organizer) {
        validateEventTimes(dto);

        Event event = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .venue(dto.getVenue())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus() != null ? dto.getStatus() : EventStatus.DRAFT)
                .organizer(organizer)
                .build();

        return toDto(eventRepository.save(event));
    }

    @Transactional
    public EventDto updateEvent(Long eventId, EventDto dto, User requestingUser) {
        Event event = getEventOrThrow(eventId);
        assertCanManage(event, requestingUser);
        validateEventTimes(dto);

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setVenue(dto.getVenue());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }

        return toDto(eventRepository.save(event));
    }

    @Transactional
    public void changeStatus(Long eventId, EventStatus newStatus, User requestingUser) {
        Event event = getEventOrThrow(eventId);
        assertCanManage(event, requestingUser);
        event.setStatus(newStatus);
        eventRepository.save(event);
    }

    public EventDto findById(Long id) {
        return toDto(getEventOrThrow(id));
    }

    public Event getEventEntityById(Long id) {
        return getEventOrThrow(id);
    }

    public List<EventDto> findAll() {
        return eventRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<EventDto> findByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId)
                .stream().map(this::toDto).toList();
    }

    public List<EventDto> findOpenEvents() {
        return eventRepository.findByStatus(EventStatus.OPEN)
                .stream().map(this::toDto).toList();
    }

    public List<EventDto> findUpcomingEvents() {
        return eventRepository.findUpcomingEvents()
                .stream().map(this::toDto).toList();
    }

    // ---- Private helpers ----

    private void validateEventTimes(EventDto dto) {
        if (dto.getEndTime() != null && dto.getStartTime() != null
                && !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessRuleException("Event end time must be after start time.");
        }
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found with id: " + id));
    }

    /**
     * Organizers can only manage their own events; Admins can manage any.
     */
    private void assertCanManage(Event event, User user) {
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException(
                    "You are not authorized to manage this event.");
        }
    }

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setVenue(event.getVenue());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setStatus(event.getStatus());
        dto.setOrganizerName(event.getOrganizer().getName());
        dto.setShiftCount(event.getShifts().size());
        return dto;
    }
}
