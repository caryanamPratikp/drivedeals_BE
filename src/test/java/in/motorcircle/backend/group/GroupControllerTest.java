package in.motorcircle.backend.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import in.motorcircle.backend.catalog.domain.Transmission;
import in.motorcircle.backend.catalog.infrastructure.CarRepository;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import in.motorcircle.backend.group.api.CreateGroupRequest;
import in.motorcircle.backend.group.api.CreateSignalPayload;
import in.motorcircle.backend.group.api.JoinGroupRequest;
import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.shared.security.JwtTokenProvider;
import in.motorcircle.backend.shared.security.UserPrincipal;
import in.motorcircle.backend.signal.domain.PurchaseMode;
import in.motorcircle.backend.signal.domain.TimelineMonths;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BuyerGroupRepository groupRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User buyer;
    private CarVariant variant;
    private BuyerGroup testGroup;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        buyer = userRepository.findByPhone("+919876543210")
                .orElseGet(() -> userRepository.save(new User(
                        "user_buyer_test", "Rahul Sharma", "rahul.grp@test.com", "+919876543210",
                        UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE
                )));

        buyerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(buyer, null));

        Car car = carRepository.save(new Car(
                "car_seltos_test", "Kia", "Seltos", BodyType.SUV, "https://example.com/seltos.jpg",
                new BigDecimal("1090000.00"), new BigDecimal("2035000.00")
        ));

        variant = variantRepository.save(new CarVariant(
                "var_seltos_gtx_test", car, "GTX Plus 1.5 Turbo DCT", FuelType.PETROL,
                Transmission.AUTOMATIC, new BigDecimal("2035000.00"), "[\"Ventilated Seats\"]"
        ));

        testGroup = groupRepository.save(new BuyerGroup(
                "grp_seltos_blr_test", car, variant, "Bengaluru", 4,
                Instant.now().plus(14, ChronoUnit.DAYS), buyer
        ));
    }

    @Test
    @DisplayName("GET /groups is public and returns paginated active groups")
    void testGetGroupsPublic() throws Exception {
        mockMvc.perform(get("/groups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /groups/{id} is public and returns group details with telemetry")
    void testGetGroupByIdPublic() throws Exception {
        mockMvc.perform(get("/groups/" + testGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testGroup.getId()))
                .andExpect(jsonPath("$.data.city").value("Bengaluru"))
                .andExpect(jsonPath("$.data.telemetry").exists());
    }

    @Test
    @DisplayName("POST /groups without authentication returns 403 Forbidden")
    void testCreateGroupUnauthenticated() throws Exception {
        CreateGroupRequest request = new CreateGroupRequest(variant.getId(), "Bengaluru", 4, null);

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /groups with Buyer token creates new group and returns 201 Created")
    void testCreateGroupAuthenticated() throws Exception {
        CreateSignalPayload signal = new CreateSignalPayload(TimelineMonths.WITHIN_30_DAYS, PurchaseMode.FINANCE, false, null, "White");
        CreateGroupRequest request = new CreateGroupRequest(variant.getId(), "Bengaluru", 3, signal);

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentMembers").value(1))
                .andExpect(jsonPath("$.data.targetMembers").value(3))
                .andExpect(jsonPath("$.data.isCurrentUserMember").value(true));
    }

    @Test
    @DisplayName("POST /groups/{id}/memberships allows authenticated buyer to join")
    void testJoinGroupAuthenticated() throws Exception {
        User anotherBuyer = userRepository.save(new User(
                "user_another_buyer", "Priya", "priya@test.com", "+919876543299",
                UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE
        ));
        String token = jwtTokenProvider.generateAccessToken(UserPrincipal.create(anotherBuyer, null));

        JoinGroupRequest request = new JoinGroupRequest(new CreateSignalPayload(
                TimelineMonths.IMMEDIATE_15_DAYS, PurchaseMode.CASH, false, null, null
        ));

        mockMvc.perform(post("/groups/" + testGroup.getId() + "/memberships")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /groups/{id}/memberships/me allows buyer to leave group")
    void testLeaveGroupAuthenticated() throws Exception {
        User anotherBuyer = userRepository.save(new User(
                "user_leave_buyer", "Karan", "karan@test.com", "+919876543288",
                UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE
        ));
        String token = jwtTokenProvider.generateAccessToken(UserPrincipal.create(anotherBuyer, null));

        // First join
        mockMvc.perform(post("/groups/" + testGroup.getId() + "/memberships")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));

        // Then leave
        mockMvc.perform(delete("/groups/" + testGroup.getId() + "/memberships/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
