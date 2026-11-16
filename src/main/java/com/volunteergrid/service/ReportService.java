package com.volunteergrid.service;

import com.volunteergrid.dto.VolunteerSummaryDto;
import com.volunteergrid.entity.AssignmentStatus;
import com.volunteergrid.repository.AssignmentRepository;
import com.volunteergrid.repository.AttendanceRepository;
import com.volunteergrid.repository.EventRepository;
import com.volunteergrid.repository.UserRepository;
import com.volunteergrid.entity.Event;
import com.volunteergrid.entity.EventStatus;
import com.volunteergrid.entity.Role;
import com.volunteergrid.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates statistics for dashboards and event reports.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AssignmentRepository assignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public ReportService(AssignmentRepository assignmentRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository,
                         EventRepository eventRepository) {
        this.assignmentRepository = assignmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Volunteer contribution summary.
     * Hours are calculated only from verified Attendance records.
     */
    public VolunteerSummaryDto getVolunteerSummary(Long volunteerId, String name) {
        List<com.volunteergrid.entity.Assignment> all =
                assignmentRepository.findByVolunteerId(volunteerId);

        long totalEvents = all.stream()
                .map(a -> a.getShift().getEvent().getId())
                .distinct().count();

        long totalShifts = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.APPROVED
                          || a.getStatus() == AssignmentStatus.COMPLETED)
                .count();

        long completed = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                .count();

        long pending = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                .count();

        long approved = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.APPROVED)
                .count();

        Double hours = attendanceRepository
                .sumCompletedHoursByVolunteerId(volunteerId);

        return VolunteerSummaryDto.builder()
                .volunteerId(volunteerId)
                .volunteerName(name)
                .totalEventsParticipated(totalEvents)
                .totalShiftsAssigned(totalShifts)
                .completedShifts(completed)
                .totalVerifiedHours(hours != null ? hours : 0.0)
                .pendingAssignments(pending)
                .approvedAssignments(approved)
                .build();
    }

    /**
     * Admin dashboard stats.
     */
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalVolunteers",
                userRepository.countByRole(com.volunteergrid.entity.Role.VOLUNTEER));
        stats.put("totalOrganizers",
                userRepository.countByRole(com.volunteergrid.entity.Role.ORGANIZER));
        stats.put("totalAssignments", assignmentRepository.count());
        return stats;
    }

    /**
     * Small, JSON-safe snapshot used by dashboard charts and manual refresh.
     * The browser receives aggregates only; entity graphs are never serialized.
     */
    public Map<String, Object> getDashboardSnapshot(User user) {
        Map<String, Object> snapshot = new HashMap<>();
        List<Event> events = user.getRole() == Role.ORGANIZER
                ? eventRepository.findByOrganizerId(user.getId())
                : eventRepository.findAll();

        Map<String, Long> eventStatuses = new HashMap<>();
        for (EventStatus status : EventStatus.values()) {
            eventStatuses.put(status.name(), events.stream()
                    .filter(event -> event.getStatus() == status)
                    .count());
        }
        snapshot.put("eventStatuses", eventStatuses);
        snapshot.put("eventCount", events.size());

        List<Map<String, Object>> eventPerformance = events.stream()
                .sorted((left, right) -> left.getStartTime().compareTo(right.getStartTime()))
                .limit(8)
                .map(event -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", event.getTitle());
                    item.put("status", event.getStatus().name());
                    item.put("registered", assignmentRepository.findByEventId(event.getId()).stream()
                            .filter(assignment -> assignment.getStatus() != AssignmentStatus.WITHDRAWN
                                    && assignment.getStatus() != AssignmentStatus.REJECTED)
                            .count());
                    item.put("shifts", event.getShifts().size());
                    return item;
                })
                .toList();
        snapshot.put("eventPerformance", eventPerformance);

        if (user.getRole() == Role.VOLUNTEER) {
            List<com.volunteergrid.entity.Assignment> assignments =
                    assignmentRepository.findByVolunteerId(user.getId());
            Map<String, Long> assignmentStatuses = new HashMap<>();
            for (AssignmentStatus status : AssignmentStatus.values()) {
                assignmentStatuses.put(status.name(), assignments.stream()
                        .filter(assignment -> assignment.getStatus() == status)
                        .count());
            }
            snapshot.put("assignmentStatuses", assignmentStatuses);
            snapshot.put("summary", getVolunteerSummary(user.getId(), user.getName()));
        } else {
            Map<String, Long> assignmentStatuses = new HashMap<>();
            for (AssignmentStatus status : AssignmentStatus.values()) {
                assignmentStatuses.put(status.name(), assignmentRepository.findAll().stream()
                        .filter(assignment -> assignment.getStatus() == status)
                        .count());
            }
            snapshot.put("assignmentStatuses", assignmentStatuses);
        }
        snapshot.put("refreshedAt", java.time.OffsetDateTime.now().toString());
        return snapshot;
    }

    /**
     * Event report: number of volunteers and attendance data.
     */
    public Map<String, Object> getEventReport(Long eventId) {
        List<com.volunteergrid.entity.Assignment> assignments =
                assignmentRepository.findByEventId(eventId);

        long totalRegistered = assignments.stream()
                .filter(a -> a.getStatus() != AssignmentStatus.WITHDRAWN
                          && a.getStatus() != AssignmentStatus.REJECTED)
                .count();

        long totalAttended = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED)
                .count();

        long checkedIn = attendanceRepository
                .countCheckedInVolunteersForEvent(eventId);

        Map<String, Object> report = new HashMap<>();
        report.put("totalRegistered", totalRegistered);
        report.put("totalAttended", totalAttended);
        report.put("totalCheckedIn", checkedIn);
        report.put("assignments", assignments);
        return report;
    }
}
