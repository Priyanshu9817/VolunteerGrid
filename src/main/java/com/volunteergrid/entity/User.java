package com.volunteergrid.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a system user.
 * A user can be an ADMIN, ORGANIZER, or VOLUNTEER.
 *
 * Why: Central identity entity.  Spring Security UserDetails is NOT
 * implemented here on purpose – we use a separate UserDetailsService
 * adapter to keep the entity clean.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Disabled users cannot log in. Admins can toggle this. */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /* ---- Relationships ---- */

    /** Events this user has organised (only meaningful for ORGANIZER role). */
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Event> organizedEvents = new ArrayList<>();

    /** Shift assignments for this user (only meaningful for VOLUNTEER role). */
    @OneToMany(mappedBy = "volunteer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Assignment> assignments = new ArrayList<>();
}
