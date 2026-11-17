package in.motorcircle.backend.quotation;

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
import in.motorcircle.backend.quotation.api.QuotationDto;
import in.motorcircle.backend.quotation.api.QuotationRevisionRequest;
import in.motorcircle.backend.quotation.api.QuotationSubmissionRequest;
import in.motorcircle.backend.quotation.api.QuotationVersionDto;
import in.motorcircle.backend.quotation.application.QuotationService;
import in.motorcircle.backend.quotation.domain.QuotationStatus;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.security.UserPrincipal;
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
class QuotationServiceTest {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private BuyerGroupRepository groupRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    private Dealer tridentHyundai;
    private BuyerGroup cretaGroup;
    private CarVariant cretaVariant;
    private User dealerUser;

    @BeforeEach
    void setUp() {
        dealerUser = userRepository.save(new User("user_d_vikram", "Vikram", "v@trident.com", "+919876543290", UserRole.DEALER, "Bengaluru", "560038", UserStatus.ACTIVE));
        tridentHyundai = new Dealer("dealer_trident_test", dealerUser, "Trident Hyundai", "Hyundai", "Bengaluru", "Indiranagar", "29AABCT1234F1Z1");
        tridentHyundai.setStatus(DealerStatus.VERIFIED);
        dealerRepository.save(tridentHyundai);

        Car car = carRepository.save(new Car("car_hyundai_test", "Hyundai", "Creta", BodyType.SUV, "https://example.com/creta.jpg", new BigDecimal("1100000.00"), new BigDecimal("2015000.00")));
        cretaVariant = variantRepository.save(new CarVariant("var_creta_test", car, "SX (O) DCT", FuelType.PETROL, Transmission.AUTOMATIC, new BigDecimal("2015000.00"), "[]"));

        User buyer = userRepository.save(new User("user_grp_creator", "Buyer", "buyer@test.com", "+919876543291", UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE));
        cretaGroup = groupRepository.save(new BuyerGroup("grp_creta_test", car, cretaVariant, "Bengaluru", 4, Instant.now().plus(14, ChronoUnit.DAYS), buyer));
        cretaGroup.setStage(GroupStage.OPEN_FOR_QUOTES);
        groupRepository.save(cretaGroup);
    }

    @Test
    @DisplayName("Submit quotation creates v1 with authoritative on-road total")
    void testSubmitQuotationVersion1() {
        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"),
                new BigDecimal("372775.00"),
                new BigDecimal("65000.00"),
                new BigDecimal("25000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("45000.00"), // Group discount
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Standard mats + cover",
                3,
                Instant.now().plus(7, ChronoUnit.DAYS),
                3
        );

        UserPrincipal principal = UserPrincipal.create(dealerUser, tridentHyundai.getId());
        QuotationDto quote = quotationService.submitQuotation(cretaGroup.getId(), request, principal);

        assertThat(quote).isNotNull();
        assertThat(quote.status()).isEqualTo(QuotationStatus.SUBMITTED.name());
        assertThat(quote.currentVersion().versionNumber()).isEqualTo(1);
        assertThat(quote.currentVersion().totalOnRoadPrice()).isEqualByComparingTo(new BigDecimal("2436275.00"));
        assertThat(quote.versionHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Revise quotation appends v2 preserving historical v1 immutable")
    void testReviseQuotationVersion2() {
        UserPrincipal principal = UserPrincipal.create(dealerUser, tridentHyundai.getId());

        // 1. Submit v1 with ₹45,000 discount
        QuotationSubmissionRequest req1 = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("372775.00"), new BigDecimal("65000.00"),
                new BigDecimal("25000.00"), new BigDecimal("3500.00"), new BigDecimal("45000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "Basic Mat", 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );
        QuotationDto quote = quotationService.submitQuotation(cretaGroup.getId(), req1, principal);

        // 2. Publish revision v2 with higher ₹60,000 discount
        QuotationRevisionRequest req2 = new QuotationRevisionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("372775.00"), new BigDecimal("65000.00"),
                new BigDecimal("25000.00"), new BigDecimal("3500.00"), new BigDecimal("60000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "Premium 3D Mats + Sunshades", 4, Instant.now().plus(7, ChronoUnit.DAYS), 2
        );
        QuotationVersionDto v2 = quotationService.reviseQuotation(quote.id(), req2, principal);

        assertThat(v2.versionNumber()).isEqualTo(2);
        assertThat(v2.groupDiscount()).isEqualByComparingTo(new BigDecimal("60000.00"));

        // 3. Inspect quote - status is REVISED, version history contains both v1 and v2
        QuotationDto updatedQuote = quotationService.getQuotationById(quote.id());
        assertThat(updatedQuote.status()).isEqualTo(QuotationStatus.REVISED.name());
        assertThat(updatedQuote.versionHistory()).hasSize(2);
        assertThat(updatedQuote.versionHistory().get(0).versionNumber()).isEqualTo(1);
        assertThat(updatedQuote.versionHistory().get(0).groupDiscount()).isEqualByComparingTo(new BigDecimal("45000.00"));
        assertThat(updatedQuote.versionHistory().get(1).versionNumber()).isEqualTo(2);
        assertThat(updatedQuote.versionHistory().get(1).groupDiscount()).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    @DisplayName("Franchise mismatch rejects quote submission")
    void testFranchiseMismatchRejected() {
        // Change dealer brand to Tata
        tridentHyundai.setBrand("Tata");
        dealerRepository.save(tridentHyundai);

        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("300000.00"), new BigDecimal("50000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );

        UserPrincipal principal = UserPrincipal.create(dealerUser, tridentHyundai.getId());
        assertThatThrownBy(() -> quotationService.submitQuotation(cretaGroup.getId(), request, principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("franchise 'Tata' does not match target group brand 'Hyundai'");
    }

    @Test
    @DisplayName("Territory mismatch rejects quote submission")
    void testTerritoryMismatchRejected() {
        // Change dealer city to Mumbai (group is in Bengaluru)
        tridentHyundai.setCity("Mumbai");
        dealerRepository.save(tridentHyundai);

        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("300000.00"), new BigDecimal("50000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );

        UserPrincipal principal = UserPrincipal.create(dealerUser, tridentHyundai.getId());
        assertThatThrownBy(() -> quotationService.submitQuotation(cretaGroup.getId(), request, principal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("city 'Mumbai' does not match target group delivery city 'Bengaluru'");
    }

    @Test
    @DisplayName("Submitting duplicate quotation without revision throws DuplicateResourceException")
    void testDuplicateQuotationRejected() {
        QuotationSubmissionRequest request = new QuotationSubmissionRequest(
                new BigDecimal("2015000.00"), new BigDecimal("300000.00"), new BigDecimal("50000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, 3, Instant.now().plus(7, ChronoUnit.DAYS), 3
        );

        UserPrincipal principal = UserPrincipal.create(dealerUser, tridentHyundai.getId());
        quotationService.submitQuotation(cretaGroup.getId(), request, principal);

        // Attempting to submit second quote for same group
        assertThatThrownBy(() -> quotationService.submitQuotation(cretaGroup.getId(), request, principal))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already been submitted for this group");
    }
}
