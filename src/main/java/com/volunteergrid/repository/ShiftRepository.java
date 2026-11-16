package com.volunteergrid.repository;

import com.volunteergrid.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByEventId(Long eventId);

    List<Shift> findByEventIdOrderByStartTimeAsc(Long eventId);

    /**
     * Count active (PENDING or APPROVED) assignments for a shift.
     * Used for capacity checks.
     */
    @Query("""
           SELECT COUNT(a) FROM Assignment a
           WHERE a.shift.id = :shiftId
             AND a.status IN ('PENDING', 'APPROVED')
           """)
    long countActiveAssignments(Long shiftId);

    /**
     * Finds shifts of a volunteer that overlap with the given time window.
     * Used for conflict detection before allowing a new registration.
     *
     * Two time windows overlap when: start1 < end2 AND end1 > start2
     */
    @Query("""
           SELECT s FROM Shift s
           JOIN s.assignments a
           WHERE a.volunteer.id = :volunteerId
             AND a.status IN ('PENDING', 'APPROVED')
             AND s.startTime < :end
             AND s.endTime   > :start
           """)
    List<Shift> findConflictingShiftsForVolunteer(Long volunteerId,
                                                  LocalDateTime start,
                                                  LocalDateTime end);
}
