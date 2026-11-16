package com.volunteergrid.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Records the actual attendance (check-in / check-out) for an Assignment.
 *
 * Business rules enforced at service layer:
 *  - checkOutTime must be AFTER checkInTime
 *  - completedHours is derived, not manually entered
 *  - Only organizers can set check-in / check-out
 */
@Entity
@Table(name = "attendance",
        indexes = {
            @Index(name = "idx_attendance_assignment", columnList = "assignment_id")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private Assignment assignment;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    /**
     * Completed hours: stored in DB, recalculated on each checkout.
     * Stored as decimal hours (e.g., 2.5 = 2 hrs 30 min).
     */
    @Column(name = "completed_hours")
    @Builder.Default
    private Double completedHours = 0.0;

    /**
     * Recalculates and stores completed hours from check-in/check-out.
     * Called by service before saving.
     */
    public void recalculateHours() {
        if (checkInTime != null && checkOutTime != null
                && checkOutTime.isAfter(checkInTime)) {
            Duration duration = Duration.between(checkInTime, checkOutTime);
            completedHours = duration.toMinutes() / 60.0;
        } else {
            completedHours = 0.0;
        }
    }
}
