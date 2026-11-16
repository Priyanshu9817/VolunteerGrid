package com.volunteergrid.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records that a volunteer has been assigned to a Shift.
 *
 * Unique constraint: one volunteer cannot have two active assignments
 * for the exact same shift (prevents duplicates at DB level).
 */
@Entity
@Table(name = "assignments",
        uniqueConstraints = {
            // A volunteer can only have ONE non-withdrawn/rejected record per shift
            @UniqueConstraint(name = "uc_assignment_volunteer_shift",
                              columnNames = {"volunteer_id", "shift_id"})
        },
        indexes = {
            @Index(name = "idx_assignment_volunteer", columnList = "volunteer_id"),
            @Index(name = "idx_assignment_shift",     columnList = "shift_id"),
            @Index(name = "idx_assignment_status",    columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private User volunteer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(name = "registration_time", nullable = false)
    @Builder.Default
    private LocalDateTime registrationTime = LocalDateTime.now();

    /* ---- Relationships ---- */

    /** Attendance record for this assignment (optional – may not exist yet). */
    @OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL,
              orphanRemoval = true, fetch = FetchType.LAZY)
    private Attendance attendance;
}
