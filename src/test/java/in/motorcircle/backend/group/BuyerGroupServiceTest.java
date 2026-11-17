package in.motorcircle.backend.group;

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
import in.motorcircle.backend.group.application.BuyerGroupService;
import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.domain.GroupStage;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.security.UserPrincipal;
import in.motorcircle.backend.signal.api.TelemetrySummaryDto;
import in.motorcircle.backend.signal.application.DemandTelemetryService;
import in.motorcircle.backend.signal.domain.PurchaseMode;
import in.motorcircle.backend.signal.domain.TimelineMonths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BuyerGroupServiceTest {

    @Autowired
    private BuyerGroupService groupService;

    @Autowired
    private BuyerGroupRepository groupRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandTelemetryService telemetryService;

    private User buyer1;
    private User buyer2;
    private User buyer3;
    private CarVariant testVariant;

    @BeforeEach
    void setUp() {
        Car car = carRepository.save(new Car(
                "car_nexon_test", "Tata", "Nexon", BodyType.SUV, "https://example.com/nexon.jpg",
                new BigDecimal("800000.00"), new BigDecimal("1500000.00")
        ));

        testVariant = variantRepository.save(new CarVariant(
                "var_nexon_fearless_test", car, "Fearless+ S DCA", FuelType.PETROL,
                Transmission.AUTOMATIC, new BigDecimal("1480000.00"), "[\"Ventilated Seats\"]"
        ));

        buyer1 = userRepository.save(new User("user_b1", "Buyer One", "b1@test.com", "+919876500001", UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE));
        buyer2 = userRepository.save(new User("user_b2", "Buyer Two", "b2@test.com", "+919876500002", UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE));
        buyer3 = userRepository.save(new User("user_b3", "Buyer Three", "b3@test.com", "+919876500003", UserRole.BUYER, "Bengaluru", "560001", UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("Create group initializes stage FORMATION and creator as member 1")
    void testCreateGroupWithInitiator() {
        CreateSignalPayload signal = new CreateSignalPayload(
                TimelineMonths.WITHIN_30_DAYS, PurchaseMode.FINANCE, false, null, "White preferred"
        );
        CreateGroupRequest request = new CreateGroupRequest(testVariant.getId(), "Bengaluru", 3, signal);

        var groupDetail = groupService.createGroup(request, UserPrincipal.create(buyer1, null));

        assertThat(groupDetail).isNotNull();
        assertThat(groupDetail.currentMembers()).isEqualTo(1);
        assertThat(groupDetail.targetMembers()).isEqualTo(3);
        assertThat(groupDetail.stage()).isEqualTo(GroupStage.FORMATION.name());
        assertThat(groupDetail.isCurrentUserMember()).isTrue();
    }

    @Test
    @DisplayName("Joining group increments member count and triggers OPEN_FOR_QUOTES when target is reached")
    void testJoinGroupStateTransition() {
        CreateGroupRequest request = new CreateGroupRequest(testVariant.getId(), "Bengaluru", 2, null);
        var groupDetail = groupService.createGroup(request, UserPrincipal.create(buyer1, null));
        String groupId = groupDetail.id();

        // Target is 2. Currently 1 member. Stage is FORMATION.
        BuyerGroup groupBefore = groupRepository.findById(groupId).orElseThrow();
        assertThat(groupBefore.getStage()).isEqualTo(GroupStage.FORMATION);
        assertThat(groupBefore.getCurrentMembers()).isEqualTo(1);

        // Buyer 2 joins -> currentMembers becomes 2 -> matches targetMembers -> transitions to OPEN_FOR_QUOTES!
        JoinGroupRequest joinRequest = new JoinGroupRequest(new CreateSignalPayload(
                TimelineMonths.IMMEDIATE_15_DAYS, PurchaseMode.CASH, true, "2018 Swift", null
        ));
        groupService.joinGroup(groupId, joinRequest, UserPrincipal.create(buyer2, null));

        BuyerGroup groupAfter = groupRepository.findById(groupId).orElseThrow();
        assertThat(groupAfter.getCurrentMembers()).isEqualTo(2);
        assertThat(groupAfter.getStage()).isEqualTo(GroupStage.OPEN_FOR_QUOTES);
    }

    @Test
    @DisplayName("Duplicate join by the same user throws DuplicateResourceException")
    void testPreventDuplicateJoin() {
        CreateGroupRequest request = new CreateGroupRequest(testVariant.getId(), "Bengaluru", 5, null);
        var groupDetail = groupService.createGroup(request, UserPrincipal.create(buyer1, null));

        // Buyer 1 attempts to join again
        assertThatThrownBy(() -> groupService.joinGroup(groupDetail.id(), null, UserPrincipal.create(buyer1, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already an active member");
    }

    @Test
    @DisplayName("Leave group marks membership LEFT and decrements count")
    void testLeaveGroup() {
        CreateGroupRequest request = new CreateGroupRequest(testVariant.getId(), "Bengaluru", 5, null);
        var groupDetail = groupService.createGroup(request, UserPrincipal.create(buyer1, null));
        String groupId = groupDetail.id();

        groupService.joinGroup(groupId, null, UserPrincipal.create(buyer2, null));
        BuyerGroup groupAfterJoin = groupRepository.findById(groupId).orElseThrow();
        assertThat(groupAfterJoin.getCurrentMembers()).isEqualTo(2);

        // Buyer 2 leaves
        groupService.leaveGroup(groupId, UserPrincipal.create(buyer2, null));
        BuyerGroup groupAfterLeave = groupRepository.findById(groupId).orElseThrow();
        assertThat(groupAfterLeave.getCurrentMembers()).isEqualTo(1);
    }

    @Test
    @DisplayName("Demand telemetry aggregates signals without exposing individual buyer PII")
    void testDemandTelemetryAggregation() {
        CreateSignalPayload s1 = new CreateSignalPayload(TimelineMonths.WITHIN_30_DAYS, PurchaseMode.FINANCE, false, null, null);
        var groupDetail = groupService.createGroup(new CreateGroupRequest(testVariant.getId(), "Bengaluru", 5, s1), UserPrincipal.create(buyer1, null));
        String groupId = groupDetail.id();

        CreateSignalPayload s2 = new CreateSignalPayload(TimelineMonths.WITHIN_30_DAYS, PurchaseMode.CASH, true, "2019 City", null);
        groupService.joinGroup(groupId, new JoinGroupRequest(s2), UserPrincipal.create(buyer2, null));

        CreateSignalPayload s3 = new CreateSignalPayload(TimelineMonths.IMMEDIATE_15_DAYS, PurchaseMode.FINANCE, true, "2017 Polo", null);
        groupService.joinGroup(groupId, new JoinGroupRequest(s3), UserPrincipal.create(buyer3, null));

        TelemetrySummaryDto telemetry = telemetryService.getAggregatedTelemetry(groupId);

        assertThat(telemetry.totalSignals()).isEqualTo(3);
        assertThat(telemetry.timelineBreakdown().get(TimelineMonths.WITHIN_30_DAYS.name())).isEqualTo(2);
        assertThat(telemetry.timelineBreakdown().get(TimelineMonths.IMMEDIATE_15_DAYS.name())).isEqualTo(1);
        assertThat(telemetry.purchaseModeBreakdown().get(PurchaseMode.FINANCE.name())).isEqualTo(2);
        assertThat(telemetry.purchaseModeBreakdown().get(PurchaseMode.CASH.name())).isEqualTo(1);
        assertThat(telemetry.exchangeCarCount()).isEqualTo(2);
    }
}
