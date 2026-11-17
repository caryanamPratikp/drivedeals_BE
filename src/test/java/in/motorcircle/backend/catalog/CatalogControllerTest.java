package in.motorcircle.backend.catalog;

import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import in.motorcircle.backend.catalog.domain.Transmission;
import in.motorcircle.backend.catalog.infrastructure.CarRepository;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository variantRepository;

    private Car creta;

    @BeforeEach
    void setUp() {
        if (!carRepository.existsById("car_test_creta")) {
            creta = new Car(
                    "car_test_creta",
                    "Hyundai",
                    "Creta",
                    BodyType.SUV,
                    "https://example.com/creta.jpg",
                    new BigDecimal("1100000.00"),
                    new BigDecimal("2015000.00")
            );
            carRepository.save(creta);

            CarVariant variant = new CarVariant(
                    "var_test_creta_sx",
                    creta,
                    "SX (O) 1.5 Turbo DCT",
                    FuelType.PETROL,
                    Transmission.AUTOMATIC,
                    new BigDecimal("2015000.00"),
                    "[\"Sunroof\", \"ADAS\"]"
            );
            variantRepository.save(variant);
        }
    }

    @Test
    @DisplayName("GET /catalogue/cars returns list of active car models")
    void testGetCars() throws Exception {
        mockMvc.perform(get("/catalogue/cars")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /catalogue/cars?brand=Hyundai filters cars by brand")
    void testGetCarsFilterByBrand() throws Exception {
        mockMvc.perform(get("/catalogue/cars?brand=Hyundai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brand").value("Hyundai"));
    }

    @Test
    @DisplayName("GET /catalogue/cars/{id} returns model specifications with trims")
    void testGetCarById() throws Exception {
        mockMvc.perform(get("/catalogue/cars/car_test_creta")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("car_test_creta"))
                .andExpect(jsonPath("$.data.model").value("Creta"))
                .andExpect(jsonPath("$.data.variants", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /catalogue/cars/{id} returns 404 ProblemDetail for non-existent vehicle")
    void testGetCarNotFound() throws Exception {
        mockMvc.perform(get("/catalogue/cars/non_existent_car_id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
