package in.motorcircle.backend.lead;

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
import in.motorcircle.backend.lead.api.UpdateLeadStatusRequest;
import in.motorcircle.backend.lead.domain.Consent;
import in.motorcircle.backend.lead.domain.ConsentStatus;
import in.motorcircle.backend.lead.domain.Lead;
import in.motorcircle.backend.lead.domain.LeadStatus;
import in.motorcircle.backend.lead.infrastructure.ConsentRepository;
import in.motorcircle.backend.lead.infrastructure.LeadRepository;
import in.motorcircle.backend.quotation.domain.Quotation;
import in.motorcircle.backend.quotation.domain.QuotationStatus;
import in.motorcircle.backend.quotation.domain.QuotationVersion;
import in.motorcircle.backend.quotation.infrastructure.QuotationRepository;
import in.motorcircle.backend.quotation.infrastructure.QuotationVersionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DealerLeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository carVariantRepository;

    @Autowired
    private BuyerGroupRepository groupRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationVersionRepository quotationVersionRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User dealerUser;
    private User buyer;
    private Dealer dealer;
    private String dealerToken;
    private String buyerToken;
    private Lead lead;

    @BeforeEach
    void setUp() {
        buyer = userRepository.save(new User(
                "usr_lead_buyer", "Pooja Hegde", "pooja@test.com", "+919877788899",
                UserRole.BUYER, "Mumbai", "400001", UserStatus.ACTIVE
        ));

        dealerUser = userRepository.save(new User(
                "usr_lead_dlr", "Dealer Rep", "rep.lead@test.com", "+919877788800",
                UserRole.DEALER, "Mumbai", "400001", UserStatus.ACTIVE
        ));

        dealer = new Dealer();
        dealer.setId("dlr_mumbai_hyundai");
        dealer.setUser(dealerUser);
        dealer.setDealershipName("Modi Hyundai Mumbai");
        dealer.setBrand("Hyundai");
        dealer.setCity("Mumbai");
        dealer.setStatus(DealerStatus.VERIFIED);
        dealer.setGstin("27HYUND1234K1Z2");
        dealer.setAddress("Andheri West, Mumbai");
        dealerRepository.save(dealer);

        dealerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(dealerUser, dealer.getId()));
        buyerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(buyer, null));

        Car car = carRepository.save(new Car("car_creta_lead", "Hyundai", "Creta", BodyType.SUV, "creta.jpg", new BigDecimal("1100000.00"), new BigDecimal("2000000.00")));
        CarVariant variant = carVariantRepository.save(new CarVariant("var_creta_sx", car, "SX (O) Turbo", FuelType.PETROL, Transmission.AUTOMATIC, new BigDecimal("1900000.00"), "Panoramic Sunroof"));

        BuyerGroup group = new BuyerGroup("grp_mumbai_creta", car, variant, "Mumbai", 5, Instant.now().plus(14, ChronoUnit.DAYS), buyer);
        group.setStage(GroupStage.OPEN_FOR_QUOTES);
        groupRepository.save(group);

        Quotation quotation = new Quotation("quote_lead_test", group, dealer);
        quotation.setStatus(QuotationStatus.SUBMITTED);
        quotation.setCurrentVersionId("ver_lead_1");
        quotationRepository.save(quotation);

        QuotationVersion version = quotationVersionRepository.save(new QuotationVersion(
                "ver_lead_1",
                quotation,
                1,
                new BigDecimal("1900000.00"),
                new BigDecimal("190000.00"),
                new BigDecimal("75000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("19000.00"),
                new BigDecimal("60000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2144000.00"),
                "Premium Dashcam",
                3,
                Instant.now().plus(7, ChronoUnit.DAYS),
                2
        ));

        Consent consent = consentRepository.save(new Consent(
                "cst_lead_test",
                buyer,
                dealer,
                quotation,
                version,
                ConsentStatus.GRANTED,
                Instant.now()
        ));

        lead = leadRepository.save(new Lead(
                "lead_pipeline_test",
                consent,
                dealer,
                buyer,
                quotation,
                version,
                LeadStatus.DEMO_DELIVERED,
                buyer.getName(),
                buyer.getPhone(),
                buyer.getEmail(),
                "Mumbai"
        ));
    }

    @Test
    @DisplayName("Dealer can retrieve isolated list of unlocked leads")
    void testDealerCanGetLeads() throws Exception {
        mockMvc.perform(get("/dealer/leads")
                        .header("Authorization", "Bearer " + dealerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].buyerName").value("Pooja Hegde"))
                .andExpect(jsonPath("$.data.content[0].buyerPhone").value("+919877788899"));
    }

    @Test
    @DisplayName("Buyer is rejected with 403 Forbidden when accessing dealer leads")
    void testBuyerCannotAccessDealerLeads() throws Exception {
        mockMvc.perform(get("/dealer/leads")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dealer can inspect single lead details")
    void testDealerCanGetLeadDetails() throws Exception {
        mockMvc.perform(get("/dealer/leads/" + lead.getId())
                        .header("Authorization", "Bearer " + dealerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(lead.getId()))
                .andExpect(jsonPath("$.data.dealerId").value(dealer.getId()));
    }

    @Test
    @DisplayName("Dealer can advance lead pipeline stage")
    void testDealerUpdateLeadStatus() throws Exception {
        UpdateLeadStatusRequest req = new UpdateLeadStatusRequest(LeadStatus.CONTACTED, "Spoke with buyer. Interested in test drive");

        mockMvc.perform(patch("/dealer/leads/" + lead.getId() + "/status")
                        .header("Authorization", "Bearer " + dealerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONTACTED"));
    }
}
