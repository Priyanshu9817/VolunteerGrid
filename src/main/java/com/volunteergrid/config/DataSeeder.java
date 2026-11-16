package com.volunteergrid.config;

import com.volunteergrid.entity.*;
import com.volunteergrid.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds demo data on application startup.
 *
 * IMPORTANT: This runs only when the "demo" profile is active.
 * Start with: java -jar app.jar --spring.profiles.active=demo
 * Or add spring.profiles.active=demo to application.properties for development.
 *
 * CREDENTIALS (development only — never use in production):
 *   admin@volunteergrid.com   / Admin@1234
 *   org1@volunteergrid.com    / Org@1234
 *   org2@volunteergrid.com    / Org@1234
 *   vol1@volunteergrid.com    / Vol@1234  (and vol2 through vol10)
 */
@Component
@Profile({"demo", "h2"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ShiftRepository shiftRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      EventRepository eventRepository,
                      ShiftRepository shiftRepository,
                      AssignmentRepository assignmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already seeded. Skipping.");
            return;
        }

        log.info("=== Seeding demo data ===");

        // ---- USERS ----
        User admin = createUser("Admin User", "admin@volunteergrid.com",
                "Admin@1234", Role.ADMIN);
        User org1  = createUser("Priya Sharma", "org1@volunteergrid.com",
                "Org@1234", Role.ORGANIZER);
        User org2  = createUser("Rohan Mehta", "org2@volunteergrid.com",
                "Org@1234", Role.ORGANIZER);

        User[] volunteers = {
            createUser("Ananya Gupta",    "vol1@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Arjun Patel",     "vol2@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Divya Nair",      "vol3@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Kabir Singh",     "vol4@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Meera Joshi",     "vol5@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Nikhil Rao",      "vol6@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Pooja Iyer",      "vol7@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Rahul Desai",     "vol8@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Sneha Kulkarni",  "vol9@volunteergrid.com",  "Vol@1234", Role.VOLUNTEER),
            createUser("Vivek Reddy",     "vol10@volunteergrid.com", "Vol@1234", Role.VOLUNTEER),
        };

        // ---- EVENT 1: TechFest (Open, organised by Priya) ----
        LocalDateTime now = LocalDateTime.now();

        Event techFest = eventRepository.save(Event.builder()
                .title("TechFest 2026")
                .description("Annual college technology festival featuring hackathons, "
                        + "robotics competitions, paper presentations, and tech exhibitions.")
                .venue("Main Campus Auditorium")
                .startTime(now.plusDays(10))
                .endTime(now.plusDays(12))
                .status(EventStatus.OPEN)
                .organizer(org1)
                .build());

        Shift tf1 = createShift(techFest, "Registration Desk",
                "Main Gate", now.plusDays(10).withHour(8),
                now.plusDays(10).withHour(11), 4);
        Shift tf2 = createShift(techFest, "Hackathon Coordination",
                "Lab Block A", now.plusDays(10).withHour(11),
                now.plusDays(10).withHour(18), 5);
        Shift tf3 = createShift(techFest, "Stall Setup & Crowd Control",
                "Exhibition Hall", now.plusDays(11).withHour(9),
                now.plusDays(11).withHour(17), 6);

        // ---- EVENT 2: Cultural Night (Open, organised by Rohan) ----
        Event culturalNight = eventRepository.save(Event.builder()
                .title("Cultural Night 2026")
                .description("An evening of music, dance, drama, and art showcasing "
                        + "the vibrant talents of our college community.")
                .venue("Open Air Theatre")
                .startTime(now.plusDays(5))
                .endTime(now.plusDays(5).withHour(22))
                .status(EventStatus.OPEN)
                .organizer(org2)
                .build());

        Shift cn1 = createShift(culturalNight, "Stage Management",
                "Open Air Theatre – Stage", now.plusDays(5).withHour(15),
                now.plusDays(5).withHour(22), 3);
        Shift cn2 = createShift(culturalNight, "Audience Management",
                "Open Air Theatre – Seating", now.plusDays(5).withHour(17),
                now.plusDays(5).withHour(22), 5);
        Shift cn3 = createShift(culturalNight, "Decoration & Setup",
                "Open Air Theatre", now.plusDays(5).withHour(10),
                now.plusDays(5).withHour(15), 4);

        // ---- EVENT 3: Industry Workshop (Completed, organised by Priya) ----
        Event workshop = eventRepository.save(Event.builder()
                .title("Industry Connect Workshop")
                .description("Half-day workshop connecting final-year students with "
                        + "industry mentors for placement preparation and career guidance.")
                .venue("Seminar Hall 2")
                .startTime(now.minusDays(3).withHour(9))
                .endTime(now.minusDays(3).withHour(14))
                .status(EventStatus.COMPLETED)
                .organizer(org1)
                .build());

        Shift ws1 = createShift(workshop, "Volunteer Coordinator",
                "Seminar Hall 2 – Entrance", now.minusDays(3).withHour(9),
                now.minusDays(3).withHour(14), 3);

        // ---- EVENT 4: Sports Carnival (Open, organised by Rohan) ----
        Event sportsCarnival = eventRepository.save(Event.builder()
                .title("Inter-College Sports Carnival")
                .description("A two-day sports meet with athletics, indoor games, and team competitions.")
                .venue("University Sports Complex")
                .startTime(now.plusDays(18).withHour(8))
                .endTime(now.plusDays(19).withHour(19))
                .status(EventStatus.OPEN)
                .organizer(org2)
                .build());
        Shift sc1 = createShift(sportsCarnival, "Team Check-in",
                "Sports Complex Entrance", now.plusDays(18).withHour(8),
                now.plusDays(18).withHour(12), 6);
        Shift sc2 = createShift(sportsCarnival, "First Aid & Support",
                "Athletics Ground", now.plusDays(18).withHour(12),
                now.plusDays(18).withHour(18), 4);

        // ---- EVENT 5: Alumni Mentorship (Draft, organised by Priya) ----
        Event alumniMeet = eventRepository.save(Event.builder()
                .title("Alumni Mentorship Day")
                .description("Mentorship circles connecting students with alumni across technology, design, and business.")
                .venue("Innovation Centre")
                .startTime(now.plusDays(28).withHour(10))
                .endTime(now.plusDays(28).withHour(17))
                .status(EventStatus.DRAFT)
                .organizer(org1)
                .build());
        Shift am1 = createShift(alumniMeet, "Mentor Welcome Desk",
                "Innovation Centre Lobby", now.plusDays(28).withHour(10),
                now.plusDays(28).withHour(13), 4);

        // ---- ASSIGNMENTS: a few sample registrations ----
        // Some volunteers for TechFest
        createAssignment(volunteers[0], tf1, AssignmentStatus.APPROVED);
        createAssignment(volunteers[1], tf1, AssignmentStatus.APPROVED);
        createAssignment(volunteers[2], tf2, AssignmentStatus.PENDING);
        createAssignment(volunteers[3], tf2, AssignmentStatus.APPROVED);
        createAssignment(volunteers[4], tf3, AssignmentStatus.PENDING);

        // Cultural Night
        createAssignment(volunteers[5], cn1, AssignmentStatus.APPROVED);
        createAssignment(volunteers[6], cn2, AssignmentStatus.APPROVED);
        createAssignment(volunteers[7], cn3, AssignmentStatus.APPROVED);

        // Completed workshop assignments
        createAssignment(volunteers[8], ws1, AssignmentStatus.COMPLETED);
        createAssignment(volunteers[9], ws1, AssignmentStatus.COMPLETED);
        createAssignment(volunteers[0], sc1, AssignmentStatus.APPROVED);
        createAssignment(volunteers[1], sc1, AssignmentStatus.PENDING);
        createAssignment(volunteers[2], sc2, AssignmentStatus.APPROVED);
        createAssignment(volunteers[3], am1, AssignmentStatus.PENDING);

        log.info("=== Demo data seeded successfully ===");
        log.info("Login credentials (development only):");
        log.info("  Admin:     admin@volunteergrid.com / Admin@1234");
        log.info("  Organizer: org1@volunteergrid.com  / Org@1234");
        log.info("  Volunteer: vol1@volunteergrid.com  / Vol@1234");
    }

    private User createUser(String name, String email, String password, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build());
    }

    private Shift createShift(Event event, String taskName, String venue,
                               LocalDateTime start, LocalDateTime end, int capacity) {
        return shiftRepository.save(Shift.builder()
                .event(event)
                .taskName(taskName)
                .venue(venue)
                .startTime(start)
                .endTime(end)
                .capacity(capacity)
                .build());
    }

    private void createAssignment(User volunteer, Shift shift, AssignmentStatus status) {
        assignmentRepository.save(Assignment.builder()
                .volunteer(volunteer)
                .shift(shift)
                .status(status)
                .registrationTime(LocalDateTime.now().minusHours(2))
                .build());
    }
}
