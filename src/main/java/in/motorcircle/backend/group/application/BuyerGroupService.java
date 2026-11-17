package in.motorcircle.backend.group.application;

import in.motorcircle.backend.catalog.api.CarDetailDto;
import in.motorcircle.backend.catalog.api.CarSummaryDto;
import in.motorcircle.backend.catalog.api.CarVariantSummaryDto;
import in.motorcircle.backend.catalog.application.CatalogService;
import in.motorcircle.backend.catalog.domain.Car;
import in.motorcircle.backend.catalog.domain.CarVariant;
import in.motorcircle.backend.catalog.infrastructure.CarVariantRepository;
import in.motorcircle.backend.notification.application.NotificationService;
import in.motorcircle.backend.group.api.*;
import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.domain.GroupMembership;
import in.motorcircle.backend.group.domain.GroupStage;
import in.motorcircle.backend.group.domain.MembershipStatus;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.group.infrastructure.GroupMembershipRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.security.UserPrincipal;
import in.motorcircle.backend.signal.api.TelemetrySummaryDto;
import in.motorcircle.backend.signal.application.DemandTelemetryService;
import in.motorcircle.backend.signal.domain.BuyerSignal;
import in.motorcircle.backend.signal.infrastructure.BuyerSignalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BuyerGroupService {

    private final BuyerGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final BuyerSignalRepository signalRepository;
    private final CarVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;
    private final DemandTelemetryService telemetryService;
    private final NotificationService notificationService;

    public BuyerGroupService(
            BuyerGroupRepository groupRepository,
            GroupMembershipRepository membershipRepository,
            BuyerSignalRepository signalRepository,
            CarVariantRepository variantRepository,
            UserRepository userRepository,
            CatalogService catalogService,
            DemandTelemetryService telemetryService,
            NotificationService notificationService
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.signalRepository = signalRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
        this.telemetryService = telemetryService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<BuyerGroupSummaryDto> getGroups(String city, String brand, String stage, Pageable pageable) {
        GroupStage stageEnum = null;
        if (stage != null && !stage.isBlank()) {
            try {
                stageEnum = GroupStage.valueOf(stage.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<BuyerGroup> groups = groupRepository.searchGroups(city, brand, stageEnum, pageable);

        return groups.map(g -> {
            Car c = g.getCar();
            CarVariant v = g.getVariant();
            CarSummaryDto carSummary = new CarSummaryDto(
                    c.getId(), c.getBrand(), c.getModel(), c.getBodyType().name(),
                    c.getImageUrl(), c.getMinPrice(), c.getMaxPrice(), 1
            );
            CarVariantSummaryDto variantSummary = new CarVariantSummaryDto(
                    v.getId(), v.getName(), v.getFuelType().name(), v.getTransmission().name(),
                    v.getExShowroomPrice(), v.getKeyFeatures()
            );
            return new BuyerGroupSummaryDto(
                    g.getId(), carSummary, variantSummary, g.getCity(),
                    g.getTargetMembers(), g.getCurrentMembers(), g.getStage().name(), g.getExpiresAt()
            );
        });
    }

    @Transactional(readOnly = true)
    public BuyerGroupDetailDto getGroupById(String groupId, String currentUserId) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerGroup", "id", groupId));

        CarDetailDto carDetail = catalogService.getCarById(group.getCar().getId());
        CarVariant v = group.getVariant();
        CarVariantSummaryDto variantSummary = new CarVariantSummaryDto(
                v.getId(), v.getName(), v.getFuelType().name(), v.getTransmission().name(),
                v.getExShowroomPrice(), v.getKeyFeatures()
        );

        boolean isMember = false;
        if (currentUserId != null) {
            isMember = membershipRepository.existsByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.ACTIVE);
        }

        TelemetrySummaryDto telemetry = telemetryService.getAggregatedTelemetry(groupId);

        return new BuyerGroupDetailDto(
                group.getId(),
                carDetail,
                variantSummary,
                group.getCity(),
                group.getTargetMembers(),
                group.getCurrentMembers(),
                group.getStage().name(),
                group.getExpiresAt(),
                isMember,
                telemetry
        );
    }

    @Transactional
    public BuyerGroupDetailDto createGroup(CreateGroupRequest request, UserPrincipal principal) {
        CarVariant variant = variantRepository.findById(request.variantId())
                .filter(CarVariant::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("CarVariant", "id", request.variantId()));

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);

        BuyerGroup group = new BuyerGroup(
                UUID.randomUUID().toString(),
                variant.getCar(),
                variant,
                request.city(),
                request.targetMembers(),
                expiresAt,
                user
        );
        group.incrementMembers(); // Initiator is member 1
        BuyerGroup savedGroup = groupRepository.save(group);

        GroupMembership membership = new GroupMembership(UUID.randomUUID().toString(), savedGroup, user);
        membershipRepository.save(membership);

        if (request.signal() != null) {
            CreateSignalPayload sig = request.signal();
            BuyerSignal signal = new BuyerSignal(
                    UUID.randomUUID().toString(),
                    savedGroup,
                    user,
                    sig.timelineMonths(),
                    sig.purchaseMode(),
                    sig.exchangeCar(),
                    sig.exchangeCarDetails(),
                    sig.notes()
            );
            signalRepository.save(signal);
        }

        notificationService.createNotification(
                user,
                "Vehicle Collective Created",
                "Your " + variant.getName() + " collective in " + request.city() + " is active. We are pooling verified buyers to unlock fleet dealership pricing.",
                "GROUP_CREATED",
                "/buyer/groups/" + savedGroup.getId()
        );

        return getGroupById(savedGroup.getId(), user.getId());
    }

    @Transactional
    public void joinGroup(String groupId, JoinGroupRequest request, UserPrincipal principal) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerGroup", "id", groupId));

        if (group.getStage() == GroupStage.CLOSED || group.getStage() == GroupStage.CANCELLED) {
            throw new BusinessRuleException("GROUP_NOT_ACTIVE", "Cannot join a group that is " + group.getStage().name());
        }

        boolean alreadyMember = membershipRepository.existsByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE);
        if (alreadyMember) {
            throw new DuplicateResourceException("You are already an active member of this group.");
        }

        if (group.getCurrentMembers() >= (int) (group.getTargetMembers() * 1.5)) {
            throw new BusinessRuleException("GROUP_CAP_REACHED", "This group has reached its maximum member capacity.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        GroupMembership membership = new GroupMembership(UUID.randomUUID().toString(), group, user);
        membershipRepository.save(membership);

        group.incrementMembers(); // Auto-transitions FORMATION -> OPEN_FOR_QUOTES if currentMembers >= targetMembers
        groupRepository.save(group);

        if (request != null && request.signal() != null) {
            CreateSignalPayload sig = request.signal();
            BuyerSignal signal = new BuyerSignal(
                    UUID.randomUUID().toString(),
                    group,
                    user,
                    sig.timelineMonths(),
                    sig.purchaseMode(),
                    sig.exchangeCar(),
                    sig.exchangeCarDetails(),
                    sig.notes()
            );
            signalRepository.save(signal);
        }

        if (group.getCreatedBy() != null && !group.getCreatedBy().getId().equals(user.getId())) {
            notificationService.createNotification(
                    group.getCreatedBy(),
                    "New Member Joined Collective",
                    user.getName() + " joined your " + group.getVariant().getName() + " collective (" + group.getCurrentMembers() + "/" + group.getTargetMembers() + " members).",
                    "MEMBER_JOINED",
                    "/buyer/groups/" + group.getId()
            );
        }

        if (group.getCurrentMembers() >= group.getTargetMembers()) {
            List<GroupMembership> members = membershipRepository.findByGroupIdAndStatus(group.getId(), MembershipStatus.ACTIVE);
            for (GroupMembership m : members) {
                if (m.getUser() != null) {
                    notificationService.createNotification(
                            m.getUser(),
                            "Collective Target Reached!",
                            "Your " + group.getVariant().getName() + " collective in " + group.getCity() + " has reached its target of " + group.getTargetMembers() + " buyers and is now open for dealership bids.",
                            "GROUP_OPEN_FOR_QUOTES",
                            "/buyer/groups/" + group.getId()
                    );
                }
            }
        }
    }

    @Transactional
    public void leaveGroup(String groupId, UserPrincipal principal) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerGroup", "id", groupId));

        GroupMembership membership = membershipRepository.findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("NOT_A_MEMBER", "You are not an active member of this group."));

        membership.markLeft();
        membershipRepository.save(membership);

        group.decrementMembers();
        groupRepository.save(group);
    }

    @Transactional
    public void submitSignal(String groupId, CreateSignalPayload payload, UserPrincipal principal) {
        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerGroup", "id", groupId));

        boolean isMember = membershipRepository.existsByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessRuleException("NOT_A_MEMBER", "You must be an active group member to submit a buyer signal.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        BuyerSignal signal = signalRepository.findByGroupIdAndUserId(groupId, principal.getId())
                .orElseGet(() -> new BuyerSignal(UUID.randomUUID().toString(), group, user, payload.timelineMonths(), payload.purchaseMode(), payload.exchangeCar(), payload.exchangeCarDetails(), payload.notes()));

        signal.setTimelineMonths(payload.timelineMonths());
        signal.setPurchaseMode(payload.purchaseMode());
        signal.setExchangeCar(payload.exchangeCar());
        signal.setExchangeCarDetails(payload.exchangeCarDetails());
        signal.setNotes(payload.notes());

        signalRepository.save(signal);
    }
}
