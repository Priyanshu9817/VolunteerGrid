package com.volunteergrid.service;

import com.volunteergrid.dto.ShiftDto;
import com.volunteergrid.entity.Event;
import com.volunteergrid.entity.Shift;
import com.volunteergrid.entity.User;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.ResourceNotFoundException;
import com.volunteergrid.exception.UnauthorizedActionException;
import com.volunteergrid.repository.AssignmentRepository;
import com.volunteergrid.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages Shift creation and listing within an Event.
 */
@Service
@Transactional(readOnly = true)
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final EventService eventService;

    public ShiftService(ShiftRepository shiftRepository,
                        AssignmentRepository assignmentRepository,
                        EventService eventService) {
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.eventService = eventService;
    }

    @Transactional
    public ShiftDto createShift(Long eventId, ShiftDto dto, User requestingUser) {
        Event event = eventService.getEventEntityById(eventId);
        assertCanManageEvent(event, requestingUser);
        validateShiftTimes(dto, event);

        Shift shift = Shift.builder()
                .event(event)
                .taskName(dto.getTaskName())
                .venue(dto.getVenue())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .capacity(dto.getCapacity())
                .build();

        return toDto(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftDto updateShift(Long shiftId, ShiftDto dto, User requestingUser) {
        Shift shift = getShiftOrThrow(shiftId);
        assertCanManageEvent(shift.getEvent(), requestingUser);
        validateShiftTimes(dto, shift.getEvent());

        long activeCount = assignmentRepository.countActiveByShiftId(shiftId);
        if (dto.getCapacity() < activeCount) {
            throw new BusinessRuleException(
                    "Cannot reduce capacity below current active registrations ("
                            + activeCount + ").");
        }

        shift.setTaskName(dto.getTaskName());
        shift.setVenue(dto.getVenue());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setCapacity(dto.getCapacity());

        return toDto(shiftRepository.save(shift));
    }

    @Transactional
    public void deleteShift(Long shiftId, User requestingUser) {
        Shift shift = getShiftOrThrow(shiftId);
        assertCanManageEvent(shift.getEvent(), requestingUser);

        long activeCount = assignmentRepository.countActiveByShiftId(shiftId);
        if (activeCount > 0) {
            throw new BusinessRuleException(
                    "Cannot delete a shift with active registrations.");
        }

        shiftRepository.delete(shift);
    }

    public ShiftDto findById(Long id) {
        Shift shift = getShiftOrThrow(id);
        ShiftDto dto = toDto(shift);
        dto.setRegisteredCount(assignmentRepository.countActiveByShiftId(id));
        return dto;
    }

    public Shift getShiftEntityById(Long id) {
        return getShiftOrThrow(id);
    }

    public List<ShiftDto> findByEvent(Long eventId) {
        return shiftRepository.findByEventIdOrderByStartTimeAsc(eventId)
                .stream()
                .map(s -> {
                    ShiftDto dto = toDto(s);
                    dto.setRegisteredCount(
                            assignmentRepository.countActiveByShiftId(s.getId()));
                    return dto;
                })
                .toList();
    }

    // ---- Private helpers ----

    private void validateShiftTimes(ShiftDto dto, Event event) {
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessRuleException("Shift end time must be after start time.");
        }
        if (dto.getStartTime().isBefore(event.getStartTime())
                || dto.getEndTime().isAfter(event.getEndTime())) {
            throw new BusinessRuleException(
                    "Shift times must be within the event's time window.");
        }
    }

    private void assertCanManageEvent(Event event, User user) {
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException(
                    "You are not authorized to manage shifts for this event.");
        }
    }

    private Shift getShiftOrThrow(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shift not found with id: " + id));
    }

    public ShiftDto toDto(Shift shift) {
        ShiftDto dto = new ShiftDto();
        dto.setId(shift.getId());
        dto.setEventId(shift.getEvent().getId());
        dto.setEventTitle(shift.getEvent().getTitle());
        dto.setTaskName(shift.getTaskName());
        dto.setVenue(shift.getVenue());
        dto.setStartTime(shift.getStartTime());
        dto.setEndTime(shift.getEndTime());
        dto.setCapacity(shift.getCapacity());
        return dto;
    }
}
