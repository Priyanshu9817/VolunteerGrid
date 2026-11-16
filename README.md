# VolunteerGrid 🎓
> **Campus Event Volunteer Management System**  
> *Project-Based Learning (PBL) Enterprise Java Application*

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-6.x-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20H2-blue.svg)](https://www.h2database.com)
[![Tests](https://img.shields.io/badge/Tests-20%2F20%20Passing-success.svg)](#running-tests)

---

## 📌 Project Overview

**VolunteerGrid** is a full-stack, role-based web application designed for colleges and universities to organize campus events, recruit and allocate student volunteers, track attendance, and record verified volunteer hours.

It features complete role-based dashboards, enforced business rules (capacity limits, shift conflict prevention, duplicate registration prevention), and automated attendance check-in/check-out.

---

## 🚀 Quick Start (Zero Setup Mode)

You need **JDK 25** and **Maven 3.9+** installed and available on your PATH.
You can then run VolunteerGrid without setting up MySQL:

### 1. Build the Project

From the project directory, run:

```powershell
mvn clean package
```

### 2. Start the Demo Server

- Run in PowerShell:
  ```powershell
  .\run-demo.ps1
  ```
- The application will boot up on: **[http://localhost:8080](http://localhost:8080)**
- H2 Web Console: **[http://localhost:8080/h2-console](http://localhost:8080/h2-console)** (JDBC URL: `jdbc:h2:mem:volunteergriddb`, User: `sa`, Password: *leave blank*)

### 3. Pre-Seeded Demo Accounts

The demo profile automatically seeds realistic test data with 3 role types:

| Role | Email | Password | What You Can Test |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@volunteergrid.com` | `Admin@1234` | Full system overview, user management, toggle enable/disable, role changes |
| **Organizer** | `org1@volunteergrid.com` | `Org@1234` | Create events, manage shifts, approve/reject volunteers, record check-in/out |
| **Volunteer** | `vol1@volunteergrid.com` | `Vol@1234` | Browse open campus events, register for shifts, view history & total hours |

*(Additional volunteers `vol2@volunteergrid.com` through `vol10@volunteergrid.com` with password `Vol@1234` are also available.)*

---

## 🧪 Running Tests

VolunteerGrid comes with a test suite covering the core business rules:

- **Double-click:** [`test.bat`](file:///c:/Users/aakas/OneDrive/Desktop/javaproject/test.bat)  
  *or run in PowerShell:*
  ```powershell
  .\test.ps1
  ```

### Test Coverage (20/20 Tests Passing):
- **EventServiceTest**: Event creation, validation, ownership checks, status transitions.
- **AssignmentServiceTest**:
  - Rule 1: Prevents registration for closed events.
  - Rule 2: Prevents duplicate registrations.
  - Rule 3: Enforces shift capacity limits.
  - Rule 4: Overlapping shift detection (prevents double-booking a volunteer).
- **AttendanceServiceTest**: Check-in, duplicate check-in prevention, check-out with automatic hours calculation.

---

## 🏗️ Architecture & Tech Stack

```
[ Browser / Thymeleaf + Bootstrap 5 UI ]
                    │ HTTP / Form Actions / CSRF
                    ▼
[ Spring Security Filter Chain (Role-Based URL Authorization) ]
                    │
[ MVC Controllers (Admin, Event, Shift, Assignment, Dashboard) ]
                    │
[ Business Service Layer (Rule validation, Conflict checking) ]
                    │
[ Spring Data JPA Repositories (Derived & Custom JPQL Queries) ]
                    │
[ Hibernate ORM ] ──► [ MySQL 8.0 (Production) / H2 In-Memory (Demo/Dev) ]
```

- **Backend:** Spring Boot 3.5.16, Java 25
- **Security:** Spring Security 6, BCrypt Password Hashing, CSRF protection, Role-based Access Control
- **Persistence:** Spring Data JPA, Hibernate 6
- **Frontend:** Thymeleaf template engine, Bootstrap 5, Bootstrap Icons
- **Database:** MySQL 8.0 (Default) / H2 In-Memory (Zero setup demo)

---

## 📁 Project Structure

```
javaproject/
├── pom.xml                                    # Maven configuration & dependencies
├── run-demo.bat / run-demo.ps1               # 1-click zero-setup demo runner
├── test.bat / test.ps1                       # 1-click unit test runner
├── README.md                                 # Documentation
└── src/
    ├── main/
    │   ├── java/com/volunteergrid/
    │   │   ├── VolunteerGridApplication.java  # Main application entry point
    │   │   ├── config/DataSeeder.java         # Realistic demo data loader
    │   │   ├── controller/                    # Spring MVC controllers
    │   │   │   ├── AdminController.java
    │   │   │   ├── AssignmentController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── EventController.java
    │   │   │   └── ShiftController.java
    │   │   ├── dto/                           # Data Transfer Objects with Bean Validation
    │   │   ├── entity/                        # JPA Entities (User, Event, Shift, Assignment, Attendance)
    │   │   ├── exception/                     # Global exception handler & business exceptions
    │   │   ├── repository/                    # Spring Data JPA repositories with JPQL conflict queries
    │   │   ├── security/                      # SecurityConfig, CustomUserDetailsService, SecurityUtils
    │   │   └── service/                       # Core business logic services
    │   └── resources/
    │       ├── application.properties         # MySQL default configuration
    │       ├── application-h2.properties      # H2 In-Memory demo configuration
    │       ├── static/css/main.css            # Custom CSS styling
    │       └── templates/                     # Thymeleaf HTML views
    └── test/                                  # Unit tests & StandaloneTestRunner
```

---

## 🗄️ Running with MySQL (Optional)

If you wish to use a local MySQL server instead of H2:
1. Ensure MySQL service is running.
2. Edit [`src/main/resources/application.properties`](file:///c:/Users/aakas/OneDrive/Desktop/javaproject/src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/volunteergrid?createDatabaseIfNotExist=true
   spring.datasource.username=YOUR_MYSQL_USER
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
3. Run the application without the `--spring.profiles.active=h2` argument.
