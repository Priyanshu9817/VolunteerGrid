package com.volunteergrid.repository;

import com.volunteergrid.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByAssignmentId(Long assignmentId);

    boolean existsByAssignmentId(Long assignmentId);

    /**
     * Sum all completed hours for a volunteer (across all events).
     */
    @Query("""
           SELECT COALESCE(SUM(att.completedHours), 0.0)
           FROM Attendance att
           JOIN att.assignment a
           WHERE a.volunteer.id = :volunteerId
             AND att.completedHours > 0
           """)
    Double sumCompletedHoursByVolunteerId(Long volunteerId);

    /**
     * Sum completed hours for a volunteer in a specific event.
     */
    @Query("""
           SELECT COALESCE(SUM(att.completedHours), 0.0)
           FROM Attendance att
           JOIN att.assignment a
           JOIN a.shift s
           WHERE a.volunteer.id = :volunteerId
             AND s.event.id = :eventId
             AND att.completedHours > 0
           """)
    Double sumCompletedHoursByVolunteerIdAndEventId(Long volunteerId, Long eventId);

    /**
     * Count of volunteers who checked in for an event.
     */
    @Query("""
           SELECT COUNT(DISTINCT a.volunteer.id)
           FROM Attendance att
           JOIN att.assignment a
           JOIN a.shift s
           WHERE s.event.id = :eventId
             AND att.checkInTime IS NOT NULL
           """)
    long countCheckedInVolunteersForEvent(Long eventId);
}
