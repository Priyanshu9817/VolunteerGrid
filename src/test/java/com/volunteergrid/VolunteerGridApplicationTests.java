package com.volunteergrid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Spring Boot Context Load Test")
class VolunteerGridApplicationTests {

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        // Verifies Spring context, entities, repositories, and configurations wire cleanly
    }
}
