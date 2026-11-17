package in.motorcircle.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MotorcircleBackendApplicationTests {

    @Test
    @DisplayName("Spring ApplicationContext loads successfully with all bean configurations")
    void contextLoads() {
        // Confirms Spring Boot 3.3.4 container boots cleanly
        assertThat(true).isTrue();
    }
}
