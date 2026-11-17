package in.motorcircle.backend.quotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.motorcircle.backend.catalog.domain.BodyType;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.domain.FuelType;
import in.motorcircle.backend.catalog.domain.Transmission;
import in.motorcircle.backend.catalog.infrastructure.CarRepository;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import in.motorcircle.backend.dealer.infrastructure.DealerRepository;
import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.domain.GroupStage;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.quotation.api.QuotationRevisionRequest;
import in.motorcircle.backend.quotation.api.QuotationSubmissionRequest;
import in.motorcircle.backend.shared.security.JwtTokenProvider;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuotationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository variantRepository;

    @Autowired
    private BuyerGroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Dealer dealer;
    private BuyerGroup group;
    private String dealerToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        User dealerUser = userRepository.save(new User("user_d_test", "Vikram", "dealer@test.com", "+919876543201", UserRole.DEALER, "Bengaluru", "560038", UserStatus.ACTIVE));
        dealer = new Dealer("dealer_hyundai_ctrl_test", dealerUser, "Trident Hyundai", "Hyundai", "Bengaluru", "Indiranagar", "29AABCT1234F1Z8");
        dealer.setStatus(DealerStatus.VERIFIED);
        dealerRepository.save(dealer);
        dealerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(dealerUser, dealer.getId()));

        User buyerUser = userRepository.save(new User("user_b_test", "Rahul", "buyer@test.com", "+919876543202", UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE));
        buyerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(buyerUser, null));

        Car car = carRepository.save(new Car("car_creta_ctrl_test", "Hyundai", "Creta", BodyType.SUV, "https://example.com/c.jpg", new BigDecimal("1100000.00"), new BigDecimal("2015000.00")));
        CarVariant variant = variantRepository.save(new CarVariant("var_creta_ctrl_test", car, "SX (O) DCT", FuelType.PETROL, Transmission.AUTOMATIC, new BigDecimal("2015000.00"), "[]"));

        group = groupRepository.save(new BuyerGroup("grp_creta_ctrl_test", car, variant, "Bengaluru", 4, Instant.now().plus(14, ChronoUnit.DAYS), buyerUser));
        group.setStage(GroupStage.OPEN_FOR_QUOTES);
        groupRepository.save(group);
    }

    @Test
    @DisplayName("GET /groups/{id}/quotations is accessible publicly")
    void testGetGroupQuotationsPublic() throws Exception {
        mockMvc.perform(get("/groups/" + group.getId() + "/quotations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /groups/{id}/quotations fails 403 Forbidden for Buyer role")
    void testBuyerCannotSubmitQuotation() throws Exception {
        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("300000.00"), new BigDecimal("50000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );

        mockMvc.perform(post("/groups/" + group.getId() + "/quotations")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /groups/{id}/quotations succeeds for verified Dealer role")
    void testDealerCanSubmitQuotation() throws Exception {
        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("372775.00"), new BigDecimal("65000.00"),
                new BigDecimal("20000.00"), new BigDecimal("3500.00"), new BigDecimal("45000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "Floor Mats", 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );

        mockMvc.perform(post("/groups/" + group.getId() + "/quotations")
                        .header("Authorization", "Bearer " + dealerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.currentVersion.versionNumber").value(1))
                .andExpect(jsonPath("$.data.currentVersion.totalOnRoadPrice").value(2431275.00));
    }
}
