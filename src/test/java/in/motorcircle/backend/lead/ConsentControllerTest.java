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
import in.motorcircle.backend.group.domain.GroupMembership;
import in.motorcircle.backend.group.domain.GroupStage;
import in.motorcircle.backend.group.domain.MembershipStatus;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.group.infrastructure.GroupMembershipRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.lead.api.GrantConsentRequest;
import in.motorcircle.backend.lead.api.RevokeConsentRequest;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConsentControllerTest {

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
    private GroupMembershipRepository membershipRepository;

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

    private User buyer;
    private User dealerUser;
    private String buyerToken;
    private String dealerToken;
    private Quotation quotation;
    private QuotationVersion version1;

    @BeforeEach
    void setUp() {
        buyer = userRepository.save(new User(
                "usr_cst_buyer", "Rahul Sharma", "rahul.cst@test.com", "+919811122233",
                UserRole.BUYER, "Delhi", "110001", UserStatus.ACTIVE
        ));

        dealerUser = userRepository.save(new User(
                "usr_cst_dlr", "Dealer Rep", "rep.cst@test.com", "+919811122244",
                UserRole.DEALER, "Delhi", "110001", UserStatus.ACTIVE
        ));

        Dealer dealer = new Dealer();
        dealer.setId("dlr_cst_test");
        dealer.setUser(dealerUser);
        dealer.setDealershipName("Delhi Prime Tata");
        dealer.setBrand("Tata");
        dealer.setCity("Delhi");
        dealer.setStatus(DealerStatus.VERIFIED);
        dealer.setGstin("07CSTTT1234F1Z5");
        dealer.setAddress("Delhi South");
        dealerRepository.save(dealer);

        buyerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(buyer, null));
        dealerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(dealerUser, dealer.getId()));

        Car car = carRepository.save(new Car("car_cst_test", "Tata", "Harrier", BodyType.SUV, "harrier.jpg", new BigDecimal("1500000.00"), new BigDecimal("2500000.00")));
        CarVariant variant = carVariantRepository.save(new CarVariant("var_cst_test", car, "Adventure Plus", FuelType.DIESEL, Transmission.AUTOMATIC, new BigDecimal("2100000.00"), "ADAS"));

        BuyerGroup group = new BuyerGroup("grp_cst_test", car, variant, "Delhi", 5, Instant.now().plus(14, ChronoUnit.DAYS), buyer);
        group.setStage(GroupStage.OPEN_FOR_QUOTES);
        groupRepository.save(group);

        GroupMembership mem = new GroupMembership("mem_cst_test", group, buyer);
        mem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mem);

        quotation = new Quotation("quote_cst_test", group, dealer);
        quotation.setStatus(QuotationStatus.SUBMITTED);
        quotation.setCurrentVersionId("ver_cst_1");
        quotationRepository.save(quotation);

        version1 = quotationVersionRepository.save(new QuotationVersion(
                "ver_cst_1",
                quotation,
                1,
                new BigDecimal("2100000.00"),
                new BigDecimal("210000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("25000.00"),
                new BigDecimal("21000.00"),
                new BigDecimal("75000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2361000.00"),
                "Matte finish kit",
                3,
                Instant.now().plus(7, ChronoUnit.DAYS),
                2
        ));
    }

    @Test
    @DisplayName("Authenticated buyer can grant consent and create lead")
    void testGrantConsentAuthenticatedBuyer() throws Exception {
        GrantConsentRequest req = new GrantConsentRequest("Looking for test drive on weekend");

        mockMvc.perform(post("/quotations/" + quotation.getId() + "/consent")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.buyerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.dealerId").value("dlr_cst_test"))
                .andExpect(jsonPath("$.data.status").value("GRANTED"));
    }

    @Test
    @DisplayName("Unauthenticated request to grant consent is rejected with 403 Forbidden")
    void testGrantConsentUnauthenticatedFails() throws Exception {
        mockMvc.perform(post("/quotations/" + quotation.getId() + "/consent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dealer cannot grant consent (restricted to BUYER)")
    void testDealerCannotGrantConsent() throws Exception {
        mockMvc.perform(post("/quotations/" + quotation.getId() + "/consent")
                        .header("Authorization", "Bearer " + dealerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Buyer can list own granted consents")
    void testGetMyConsents() throws Exception {
        mockMvc.perform(get("/consents/me")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Buyer can revoke consent successfully")
    void testRevokeConsent() throws Exception {
        Consent consent = consentRepository.save(new Consent(
                "cst_rvk_test",
                buyer,
                dealerRepository.findById("dlr_cst_test").get(),
                quotation,
                version1,
                ConsentStatus.GRANTED,
                Instant.now()
        ));

        leadRepository.save(new Lead(
                "lead_rvk_test",
                consent,
                dealerRepository.findById("dlr_cst_test").get(),
                buyer,
                quotation,
                version1,
                LeadStatus.DEMO_DELIVERED,
                buyer.getName(),
                buyer.getPhone(),
                buyer.getEmail(),
                "Delhi"
        ));

        RevokeConsentRequest req = new RevokeConsentRequest("Found better financing terms elsewhere");

        mockMvc.perform(post("/consents/" + consent.getId() + "/revoke")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
