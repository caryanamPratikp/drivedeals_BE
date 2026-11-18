package in.motorcircle.backend.config;

import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import in.motorcircle.backend.catalog.domain.Transmission;
import in.motorcircle.backend.catalog.infrastructure.CarRepository;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CarRepository carRepository;
    private final CarVariantRepository variantRepository;
    private final UserRepository userRepository;

    public DatabaseSeeder(
            CarRepository carRepository,
            CarVariantRepository variantRepository,
            UserRepository userRepository
    ) {
        this.carRepository = carRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Ensure Admin user always exists
        if (userRepository.findByPhone("+919665879804").isEmpty()) {
            User admin = new User(
                    "admin_user_primary",
                    "Platform Administrator",
                    "admin.919665879804@motorcircle.in",
                    "+919665879804",
                    UserRole.ADMIN,
                    "Pune",
                    "411001",
                    UserStatus.ACTIVE
            );
            userRepository.save(admin);
            log.info("[DATABASE_SEEDER] Initialized Admin User with mobile number +919665879804");
        }

        log.info("[DATABASE_SEEDER] Database ready with 0 dummy cars, 0 dummy groups, and 0 dummy users.");
    }
}
