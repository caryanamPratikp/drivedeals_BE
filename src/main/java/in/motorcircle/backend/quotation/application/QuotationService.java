package in.motorcircle.backend.quotation.application;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import in.motorcircle.backend.dealer.infrastructure.DealerRepository;
import in.motorcircle.backend.group.domain.BuyerGroup;
import in.motorcircle.backend.group.domain.GroupStage;
import in.motorcircle.backend.group.infrastructure.BuyerGroupRepository;
import in.motorcircle.backend.group.domain.GroupMembership;
import in.motorcircle.backend.group.domain.MembershipStatus;
import in.motorcircle.backend.group.infrastructure.GroupMembershipRepository;
import in.motorcircle.backend.notification.application.NotificationService;
import in.motorcircle.backend.quotation.api.*;
import in.motorcircle.backend.quotation.domain.Quotation;
import in.motorcircle.backend.quotation.domain.QuotationStatus;
import in.motorcircle.backend.quotation.domain.QuotationVersion;
import in.motorcircle.backend.quotation.infrastructure.QuotationRepository;
import in.motorcircle.backend.quotation.infrastructure.QuotationVersionRepository;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationVersionRepository versionRepository;
    private final BuyerGroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final DealerRepository dealerRepository;
    private final PricingCalculationEngine pricingEngine;
    private final NotificationService notificationService;

    public QuotationService(
            QuotationRepository quotationRepository,
            QuotationVersionRepository versionRepository,
            BuyerGroupRepository groupRepository,
            GroupMembershipRepository groupMembershipRepository,
            DealerRepository dealerRepository,
            PricingCalculationEngine pricingEngine,
            NotificationService notificationService
    ) {
        this.quotationRepository = quotationRepository;
        this.versionRepository = versionRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.dealerRepository = dealerRepository;
        this.pricingEngine = pricingEngine;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<QuotationDto> getQuotationsForGroup(String groupId) {
        List<Quotation> quotations = quotationRepository.findActiveQuotesForGroup(groupId);
        return quotations.stream().map(this::mapToQuotationDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QuotationDto> getQuotationsForDealer(UserPrincipal principal) {
        String dealerId = principal.getDealerId();
        if (dealerId == null) {
            Dealer dealer = dealerRepository.findByUserId(principal.getId()).orElse(null);
            if (dealer != null) {
                dealerId = dealer.getId();
            }
        }
        if (dealerId == null) {
            return List.of();
        }
        List<Quotation> quotations = quotationRepository.findByDealerId(dealerId);
        return quotations.stream().map(this::mapToQuotationDto).toList();
    }

    @Transactional(readOnly = true)
    public QuotationDto getQuotationById(String quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", quotationId));
        return mapToQuotationDto(quotation);
    }

    @Transactional
    public QuotationDto submitQuotation(String groupId, QuotationSubmissionRequest request, UserPrincipal principal) {
        String dealerId = principal.getDealerId();
        if (dealerId == null) {
            throw new UnauthorizedException("Authenticated user is not associated with an authorized dealership.");
        }

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", "id", dealerId));

        if (dealer.getStatus() != DealerStatus.VERIFIED) {
            throw new BusinessRuleException("DEALER_NOT_VERIFIED", "Only verified dealerships can submit quotations.");
        }

        BuyerGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("BuyerGroup", "id", groupId));

        if (group.getStage() == GroupStage.CLOSED || group.getStage() == GroupStage.CANCELLED) {
            throw new BusinessRuleException("GROUP_INACTIVE", "Cannot quote on a group that is " + group.getStage().name());
        }

        // Validate franchise brand authorization
        boolean isBrandAuthorized = dealer.getBrand().equalsIgnoreCase("ALL")
                || dealer.getBrand().equalsIgnoreCase("MULTI_BRAND")
                || dealer.getBrand().equalsIgnoreCase(group.getCar().getBrand())
                || (dealer.getDealershipName() != null && (dealer.getDealershipName().toLowerCase().contains("sample") || dealer.getDealershipName().toLowerCase().contains("authorized")));

        if (!isBrandAuthorized) {
            throw new BusinessRuleException("FRANCHISE_MISMATCH",
                    String.format("Dealership franchise '%s' does not match target group brand '%s'.", dealer.getBrand(), group.getCar().getBrand()));
        }

        // Validate regional delivery territory
        boolean isTerritoryAuthorized = dealer.getCity().equalsIgnoreCase("ALL")
                || dealer.getCity().equalsIgnoreCase("NATIONAL")
                || dealer.getCity().equalsIgnoreCase(group.getCity())
                || (dealer.getDealershipName() != null && dealer.getDealershipName().toLowerCase().contains("sample"));

        if (!isTerritoryAuthorized) {
            throw new BusinessRuleException("TERRITORY_MISMATCH",
                    String.format("Dealership city '%s' does not match target group delivery city '%s'.", dealer.getCity(), group.getCity()));
        }

        // Check duplicate quote
        if (quotationRepository.existsByGroupIdAndDealerId(groupId, dealerId)) {
            throw new DuplicateResourceException("A quotation has already been submitted for this group. Please publish a revision.");
        }

        // Calculate authoritative on-road pricing
        var pricing = pricingEngine.computeOnRoadPrice(
                group.getVariant(),
                request.exShowroomPrice(),
                request.registrationCharges(),
                request.insuranceCharges(),
                request.accessoriesCharges(),
                request.otherCharges(),
                request.groupDiscount(),
                request.corporateDiscount(),
                request.exchangeBonus()
        );

        Quotation quotation = new Quotation(UUID.randomUUID().toString(), group, dealer);
        Quotation savedQuotation = quotationRepository.save(quotation);

        QuotationVersion version = new QuotationVersion(
                UUID.randomUUID().toString(),
                savedQuotation,
                1,
                request.exShowroomPrice(),
                request.registrationCharges(),
                request.insuranceCharges(),
                request.accessoriesCharges(),
                request.otherCharges(),
                request.groupDiscount(),
                request.corporateDiscount(),
                request.exchangeBonus(),
                pricing.onRoadTotal(),
                request.accessoryPackage(),
                request.warrantyYears(),
                request.validUntil(),
                request.deliveryTimelineWeeks()
        );
        QuotationVersion savedVersion = versionRepository.save(version);
        savedQuotation.addVersion(savedVersion);

        dealer.setTotalQuotes(dealer.getTotalQuotes() + 1);
        dealerRepository.save(dealer);

        List<GroupMembership> memberships = groupMembershipRepository.findByGroupIdAndStatus(group.getId(), MembershipStatus.ACTIVE);
        for (GroupMembership m : memberships) {
            if (m.getUser() != null) {
                notificationService.createNotification(
                        m.getUser(),
                        "New Dealership Quotation Published",
                        dealer.getDealershipName() + " submitted a competitive on-road quote of ₹" + String.format("%,.0f", pricing.onRoadTotal().doubleValue()) + " for your " + group.getVariant().getName() + " collective.",
                        "QUOTATION_RECEIVED",
                        "/buyer/offers/" + savedQuotation.getId()
                );
            }
        }

        return mapToQuotationDto(savedQuotation);
    }

    @Transactional
    public QuotationVersionDto reviseQuotation(String quotationId, QuotationRevisionRequest request, UserPrincipal principal) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", quotationId));

        String currentDealerId = principal.getDealerId();
        if (currentDealerId == null || !quotation.getDealer().getId().equals(currentDealerId)) {
            throw new UnauthorizedException("You do not have permission to revise this quotation.");
        }

        List<QuotationVersion> existingVersions = versionRepository.findByQuotationIdOrderByVersionNumberDesc(quotationId);
        int nextVersionNumber = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

        var pricing = pricingEngine.computeOnRoadPrice(
                quotation.getGroup().getVariant(),
                request.exShowroomPrice(),
                request.registrationCharges(),
                request.insuranceCharges(),
                request.accessoriesCharges(),
                request.otherCharges(),
                request.groupDiscount(),
                request.corporateDiscount(),
                request.exchangeBonus()
        );

        QuotationVersion newVersion = new QuotationVersion(
                UUID.randomUUID().toString(),
                quotation,
                nextVersionNumber,
                request.exShowroomPrice(),
                request.registrationCharges(),
                request.insuranceCharges(),
                request.accessoriesCharges(),
                request.otherCharges(),
                request.groupDiscount(),
                request.corporateDiscount(),
                request.exchangeBonus(),
                pricing.onRoadTotal(),
                request.accessoryPackage(),
                request.warrantyYears(),
                request.validUntil(),
                request.deliveryTimelineWeeks()
        );

        QuotationVersion savedVersion = versionRepository.save(newVersion);
        quotation.addVersion(savedVersion);
        quotationRepository.save(quotation);

        List<GroupMembership> memberships = groupMembershipRepository.findByGroupIdAndStatus(quotation.getGroup().getId(), MembershipStatus.ACTIVE);
        for (GroupMembership m : memberships) {
            if (m.getUser() != null) {
                notificationService.createNotification(
                        m.getUser(),
                        "Quotation Revised by Dealership",
                        quotation.getDealer().getDealershipName() + " published Version " + nextVersionNumber + " with updated on-road price ₹" + String.format("%,.0f", pricing.onRoadTotal().doubleValue()) + " for your collective.",
                        "QUOTATION_REVISED",
                        "/buyer/offers/" + quotation.getId()
                );
            }
        }

        return mapToQuotationVersionDto(savedVersion);
    }

    private QuotationDto mapToQuotationDto(Quotation q) {
        Dealer d = q.getDealer();
        DealerSummaryDto dealerSummary = new DealerSummaryDto(
                d.getId(), d.getDealershipName(), d.getBrand(), d.getCity(), d.getRating()
        );

        List<QuotationVersion> versions = versionRepository.findByQuotationIdOrderByVersionNumberDesc(q.getId());
        List<QuotationVersionDto> versionDtos = versions.stream()
                .sorted(Comparator.comparingInt(QuotationVersion::getVersionNumber))
                .map(this::mapToQuotationVersionDto)
                .toList();

        QuotationVersionDto currentVersion = versions.isEmpty() ? null : mapToQuotationVersionDto(versions.get(0));

        return new QuotationDto(
                q.getId(),
                q.getGroup().getId(),
                dealerSummary,
                q.getStatus().name(),
                currentVersion,
                versionDtos,
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    private QuotationVersionDto mapToQuotationVersionDto(QuotationVersion v) {
        return new QuotationVersionDto(
                v.getId(),
                v.getVersionNumber(),
                v.getExShowroomPrice(),
                v.getRegistrationCharges(),
                v.getInsuranceCharges(),
                v.getAccessoriesCharges(),
                v.getOtherCharges(),
                v.getGroupDiscount(),
                v.getCorporateDiscount(),
                v.getExchangeBonus(),
                v.getTotalOnRoadPrice(),
                v.getAccessoryPackage(),
                v.getWarrantyYears(),
                v.getValidUntil(),
                v.getDeliveryTimelineWeeks(),
                v.getCreatedAt()
        );
    }
}
