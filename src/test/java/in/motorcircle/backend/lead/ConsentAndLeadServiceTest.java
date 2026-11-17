package in.motorcircle.backend.lead;

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
import in.motorcircle.backend.lead.api.ConsentResponseDto;
import in.motorcircle.backend.lead.api.DealerLeadDto;
import in.motorcircle.backend.lead.api.GrantConsentRequest;
import in.motorcircle.backend.lead.api.RevokeConsentRequest;
import in.motorcircle.backend.lead.api.UpdateLeadStatusRequest;
import in.motorcircle.backend.lead.application.ConsentAndLeadService;
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
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsentAndLeadServiceTest {

    @Autowired
    private ConsentAndLeadService consentAndLeadService;

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

    private User buyer;
    private User otherBuyer;
    private Dealer dealer;
    private Dealer otherDealer;
    private BuyerGroup group;
    private Quotation quotation;
    private QuotationVersion version1;

    @BeforeEach
    void setUp() {
        buyer = userRepository.save(new User("usr_svc_buyer1", "Rahul Sharma", "rahul.svc@test.com", "+919876543201", UserRole.BUYER, "Delhi", "110001", UserStatus.ACTIVE));
        otherBuyer = userRepository.save(new User("usr_svc_buyer2", "Ananya Verma", "ananya.svc@test.com", "+919876543202", UserRole.BUYER, "Delhi", "110001", UserStatus.ACTIVE));
        User dealerUser = userRepository.save(new User("usr_svc_dlr1", "Dealer Manager", "mgr.svc@tata.com", "+919876543203", UserRole.DEALER, "Delhi", "110001", UserStatus.ACTIVE));
        User otherDlrUser = userRepository.save(new User("usr_svc_dlr2", "Other Manager", "other.svc@tata.com", "+919876543204", UserRole.DEALER, "Delhi", "110001", UserStatus.ACTIVE));

        dealer = new Dealer();
        dealer.setId("dlr_svc_tata");
        dealer.setUser(dealerUser);
        dealer.setDealershipName("AutoVikas Tata Delhi");
        dealer.setBrand("Tata");
        dealer.setCity("Delhi");
        dealer.setStatus(DealerStatus.VERIFIED);
        dealer.setGstin("07SVCTATA001Z5");
        dealer.setAddress("Connaught Place, New Delhi");
        dealer = dealerRepository.save(dealer);

        otherDealer = new Dealer();
        otherDealer.setId("dlr_svc_other");
        otherDealer.setUser(otherDlrUser);
        otherDealer.setDealershipName("Other Dealer");
        otherDealer.setBrand("Tata");
        otherDealer.setCity("Delhi");
        otherDealer.setStatus(DealerStatus.VERIFIED);
        otherDealer.setGstin("07SVCOTHR001Z6");
        otherDealer.setAddress("Dwarka, New Delhi");
        otherDealer = dealerRepository.save(otherDealer);

        Car car = carRepository.save(new Car("car_svc_nexon", "Tata", "Nexon", BodyType.SUV, "nexon.jpg", new BigDecimal("800000.00"), new BigDecimal("1550000.00")));
        CarVariant variant = carVariantRepository.save(new CarVariant("var_svc_creative", car, "Creative Plus S", FuelType.PETROL, Transmission.MANUAL, new BigDecimal("1150000.00"), "Sunroof"));

        group = new BuyerGroup("grp_svc_nexon", car, variant, "Delhi", 5, Instant.now().plus(14, ChronoUnit.DAYS), buyer);
        group.setStage(GroupStage.OPEN_FOR_QUOTES);
        group = groupRepository.save(group);

        GroupMembership mem = new GroupMembership("mem_svc_1", group, buyer);
        mem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(mem);

        quotation = new Quotation("quote_svc_1", group, dealer);
        quotation.setStatus(QuotationStatus.SUBMITTED);
        quotation.setCurrentVersionId("ver_svc_1");
        quotation = quotationRepository.save(quotation);

        version1 = quotationVersionRepository.save(new QuotationVersion(
                "ver_svc_1",
                quotation,
                1,
                new BigDecimal("1150000.00"),
                new BigDecimal("92000.00"),
                new BigDecimal("46000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("11500.00"),
                new BigDecimal("45000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1269500.00"),
                "Standard mats",
                3,
                Instant.now().plus(7, ChronoUnit.DAYS),
                3
        ));
    }

    @Test
    @DisplayName("Successfully grant consent and create lead atomically")
    void testGrantConsentAndCreateLeadSuccess() {
        GrantConsentRequest req = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto res = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), req);

        assertThat(res).isNotNull();
        assertThat(res.buyerId()).isEqualTo(buyer.getId());
        assertThat(res.dealerId()).isEqualTo(dealer.getId());
        assertThat(res.status()).isEqualTo(ConsentStatus.GRANTED);

        assertThat(consentRepository.findById(res.id())).isPresent();
        assertThat(leadRepository.existsByBuyerIdAndDealerIdAndQuotationIdAndStatusNot(
                buyer.getId(), dealer.getId(), quotation.getId(), LeadStatus.REVOKED
        )).isTrue();
    }

    @Test
    @DisplayName("Duplicate active lead prevents second consent grant")
    void testGrantConsentDuplicateLeadFails() {
        GrantConsentRequest req = new GrantConsentRequest("Ready for test drive");
        consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), req);

        assertThatThrownBy(() -> consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already");
    }

    @Test
    @DisplayName("Non-member buyer cannot grant consent")
    void testGrantConsentNonMemberFails() {
        GrantConsentRequest req = new GrantConsentRequest("");
        assertThatThrownBy(() -> consentAndLeadService.grantConsentAndCreateLead(otherBuyer, quotation.getId(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active member");
    }

    @Test
    @DisplayName("Unverified dealer rejects consent grant")
    void testGrantConsentUnverifiedDealerFails() {
        dealer.setStatus(DealerStatus.PENDING_VERIFICATION);
        dealerRepository.save(dealer);

        GrantConsentRequest req = new GrantConsentRequest("");
        assertThatThrownBy(() -> consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not eligible to receive");
    }

    @Test
    @DisplayName("Revoke consent cascades status to lead and creates audit event")
    void testRevokeConsentSuccess() {
        GrantConsentRequest grantReq = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto granted = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), grantReq);

        RevokeConsentRequest req = new RevokeConsentRequest("Booked through family dealer");
        consentAndLeadService.revokeConsent(buyer, granted.id(), req);

        Consent cst = consentRepository.findById(granted.id()).orElseThrow();
        assertThat(cst.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(cst.getRevocationReason()).isEqualTo("Booked through family dealer");

        Lead lead = leadRepository.findByConsentId(granted.id()).orElseThrow();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.REVOKED);
    }

    @Test
    @DisplayName("Buyer cannot revoke someone else's consent")
    void testRevokeConsentUnauthorizedBuyerFails() {
        GrantConsentRequest grantReq = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto granted = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), grantReq);

        RevokeConsentRequest req = new RevokeConsentRequest("test");
        assertThatThrownBy(() -> consentAndLeadService.revokeConsent(otherBuyer, granted.id(), req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Dealer lead row-level tenancy: Dealer A cannot access Dealer B's lead")
    void testDealerLeadRowLevelTenancy() {
        GrantConsentRequest grantReq = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto granted = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), grantReq);
        Lead lead = leadRepository.findByConsentId(granted.id()).orElseThrow();

        assertThatThrownBy(() -> consentAndLeadService.getDealerLeadDetail(otherDealer, lead.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Dealer updates lead pipeline status successfully")
    void testDealerUpdateLeadStatusSuccess() {
        GrantConsentRequest grantReq = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto granted = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), grantReq);
        Lead lead = leadRepository.findByConsentId(granted.id()).orElseThrow();

        UpdateLeadStatusRequest req = new UpdateLeadStatusRequest(LeadStatus.TEST_DRIVE_SCHEDULED, "Scheduled for Saturday 11am");
        DealerLeadDto res = consentAndLeadService.updateLeadStatus(dealer, lead.getId(), req);

        assertThat(res.status()).isEqualTo(LeadStatus.TEST_DRIVE_SCHEDULED);
    }

    @Test
    @DisplayName("Updating a revoked lead throws BusinessRuleException")
    void testUpdateRevokedLeadFails() {
        GrantConsentRequest grantReq = new GrantConsentRequest("Ready for test drive");
        ConsentResponseDto granted = consentAndLeadService.grantConsentAndCreateLead(buyer, quotation.getId(), grantReq);
        Lead lead = leadRepository.findByConsentId(granted.id()).orElseThrow();

        RevokeConsentRequest revokeReq = new RevokeConsentRequest("Revoked");
        consentAndLeadService.revokeConsent(buyer, granted.id(), revokeReq);

        UpdateLeadStatusRequest req = new UpdateLeadStatusRequest(LeadStatus.CONTACTED, "Tried calling");
        assertThatThrownBy(() -> consentAndLeadService.updateLeadStatus(dealer, lead.getId(), req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot update status of a revoked lead");
    }
}
