package com.volunteergrid.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Shift is a specific task/duty block within an Event.
 * Each Shift has a capacity (max number of volunteers).
 *
 * e.g. "Registration Desk" at TechFest on Nov 5, 9:00–12:00, capacity 5
 */
@Entity
@Table(name = "shifts",
        indexes = {
            @Index(name = "idx_shift_event", columnList = "event_id"),
            @Index(name = "idx_shift_time", columnList = "start_time, end_time")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String venue;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * Maximum number of volunteers for this shift.
     * Must be at least 1.
     */
    @Min(1)
    @Column(nullable = false)
    @Builder.Default
    private int capacity = 1;

    /* ---- Relationships ---- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Assignment> assignments = new ArrayList<>();

    /**
     * Convenience: counts APPROVED + PENDING assignments (active).
     * Called from service layer – not a DB query.
     */
    public long getActiveAssignmentCount() {
        return assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.APPROVED
                          || a.getStatus() == AssignmentStatus.PENDING)
                .count();
    }
}
