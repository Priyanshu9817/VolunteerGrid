package com.volunteergrid.repository;

import com.volunteergrid.entity.Assignment;
import com.volunteergrid.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByVolunteerId(Long volunteerId);

    List<Assignment> findByShiftId(Long shiftId);

    List<Assignment> findByShiftIdAndStatus(Long shiftId, AssignmentStatus status);

    Optional<Assignment> findByVolunteerIdAndShiftId(Long volunteerId, Long shiftId);

    boolean existsByVolunteerIdAndShiftId(Long volunteerId, Long shiftId);

    /**
     * All assignments for a volunteer with a given status.
     */
    List<Assignment> findByVolunteerIdAndStatus(Long volunteerId, AssignmentStatus status);

    /**
     * Volunteer's assignments for a specific event (via shift → event).
     */
    @Query("""
           SELECT a FROM Assignment a
           JOIN a.shift s
           WHERE a.volunteer.id = :volunteerId
             AND s.event.id = :eventId
           """)
    List<Assignment> findByVolunteerIdAndEventId(Long volunteerId, Long eventId);

    /**
     * All assignments for all shifts in a given event.
     */
    @Query("""
           SELECT a FROM Assignment a
           JOIN a.shift s
           WHERE s.event.id = :eventId
           ORDER BY a.registrationTime DESC
           """)
    List<Assignment> findByEventId(Long eventId);

    /**
     * Count approved assignments for a shift (for capacity check).
     */
    @Query("""
           SELECT COUNT(a) FROM Assignment a
           WHERE a.shift.id = :shiftId
             AND a.status IN ('PENDING', 'APPROVED')
           """)
    long countActiveByShiftId(Long shiftId);
}
