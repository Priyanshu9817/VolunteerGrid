package com.volunteergrid.repository;

import com.volunteergrid.entity.Event;
import com.volunteergrid.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizerId(Long organizerId);

    List<Event> findByStatus(EventStatus status);

    List<Event> findByStatusIn(List<EventStatus> statuses);

    List<Event> findByOrganizerIdAndStatus(Long organizerId, EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status IN ('OPEN', 'DRAFT') ORDER BY e.startTime ASC")
    List<Event> findUpcomingEvents();
}
