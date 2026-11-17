package in.motorcircle.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MotorcircleBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotorcircleBackendApplication.class, args);
    }

    @PostConstruct
    void init() {
        // Enforce UTC timezone globally across all application layers
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String BOLD = "\u001B[1m";
        String GREEN_BOLD = "\u001B[1;32m";
        String CYAN_BOLD = "\u001B[1;36m";
        String RESET = "\u001B[0m";

        System.out.println();
        System.out.println(GREEN_BOLD + "================================================================================" + RESET);
        System.out.println(GREEN_BOLD + "  " + BOLD + "DRIVEDEALS BACKEND STARTED SUCCESSFULLY" + RESET);
        System.out.println(GREEN_BOLD + "================================================================================" + RESET);
        System.out.println(BOLD + "  * Local API Base:    " + CYAN_BOLD + "http://localhost:8080/api/v1" + RESET);
        System.out.println(BOLD + "  * Swagger UI Docs:   " + CYAN_BOLD + "http://localhost:8080/api/v1/swagger-ui.html" + RESET);
        System.out.println(BOLD + "  * Database (MySQL):  " + CYAN_BOLD + "localhost:3306 (drivedeals) - CONNECTED" + RESET);
        System.out.println(BOLD + "  * OTP Mode:          " + CYAN_BOLD + "Direct In-Memory DB Session (SMS Disabled)" + RESET);
        System.out.println(GREEN_BOLD + "================================================================================" + RESET);
        System.out.println();
    }
}
