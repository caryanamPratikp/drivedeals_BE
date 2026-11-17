package in.motorcircle.backend.lead.application;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import in.motorcircle.backend.group.domain.GroupMembership;
import in.motorcircle.backend.group.domain.MembershipStatus;
import in.motorcircle.backend.group.infrastructure.GroupMembershipRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.lead.api.ConsentResponseDto;
import in.motorcircle.backend.lead.api.DealerLeadDto;
import in.motorcircle.backend.lead.api.GrantConsentRequest;
import in.motorcircle.backend.lead.api.LeadEventDto;
import in.motorcircle.backend.lead.api.RevokeConsentRequest;
import in.motorcircle.backend.lead.api.UpdateLeadStatusRequest;
import in.motorcircle.backend.lead.domain.Consent;
import in.motorcircle.backend.lead.domain.ConsentStatus;
import in.motorcircle.backend.lead.domain.Lead;
import in.motorcircle.backend.lead.domain.LeadEvent;
import in.motorcircle.backend.lead.domain.LeadStatus;
import in.motorcircle.backend.lead.infrastructure.ConsentRepository;
import in.motorcircle.backend.lead.infrastructure.LeadEventRepository;
import in.motorcircle.backend.lead.infrastructure.LeadRepository;
import in.motorcircle.backend.notification.application.NotificationService;
import in.motorcircle.backend.quotation.domain.Quotation;
import in.motorcircle.backend.quotation.domain.QuotationStatus;
import in.motorcircle.backend.quotation.domain.QuotationVersion;
import in.motorcircle.backend.quotation.infrastructure.QuotationRepository;
import in.motorcircle.backend.quotation.infrastructure.QuotationVersionRepository;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import in.motorcircle.backend.shared.exception.DuplicateResourceException;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ConsentAndLeadService {

    private static final Logger log = LoggerFactory.getLogger(ConsentAndLeadService.class);

    private final ConsentRepository consentRepository;
    private final LeadRepository leadRepository;
    private final LeadEventRepository leadEventRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationVersionRepository quotationVersionRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final NotificationService notificationService;

    public ConsentAndLeadService(
            ConsentRepository consentRepository,
            LeadRepository leadRepository,
            LeadEventRepository leadEventRepository,
            QuotationRepository quotationRepository,
            QuotationVersionRepository quotationVersionRepository,
            GroupMembershipRepository groupMembershipRepository,
            NotificationService notificationService
    ) {
        this.consentRepository = consentRepository;
        this.leadRepository = leadRepository;
        this.leadEventRepository = leadEventRepository;
        this.quotationRepository = quotationRepository;
        this.quotationVersionRepository = quotationVersionRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.notificationService = notificationService;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ConsentResponseDto grantConsentAndCreateLead(User buyer, String quotationId, GrantConsentRequest request) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", quotationId));

        if (quotation.getStatus() != QuotationStatus.SUBMITTED && quotation.getStatus() != QuotationStatus.REVISED) {
            throw new BusinessRuleException("INVALID_QUOTATION_STATUS", "Quotation is not active for buyer consent.");
        }

        Dealer dealer = quotation.getDealer();
        if (dealer.getStatus() != DealerStatus.VERIFIED) {
            throw new BusinessRuleException("DEALER_NOT_VERIFIED", "Dealership is not eligible to receive buyer leads.");
        }

        var group = quotation.getGroup();
        boolean isMember = groupMembershipRepository.existsByGroupIdAndUserIdAndStatus(group.getId(), buyer.getId(), MembershipStatus.ACTIVE);
        if (!isMember) {
            throw new BusinessRuleException("NOT_GROUP_MEMBER", "You must be an active member of the demand group to grant consent.");
        }

        Optional<Consent> existingConsent = consentRepository.findByUserIdAndQuotationId(buyer.getId(), quotation.getId());
        if (existingConsent.isPresent() && existingConsent.get().getStatus() == ConsentStatus.GRANTED) {
            log.info("[CONSENT_IDEMPOTENT] Existing active consent [{}] found for buyer [{}] on quotation [{}]",
                    existingConsent.get().getId(), buyer.getId(), quotation.getId());
            Optional<Lead> leadOpt = leadRepository.findByConsentId(existingConsent.get().getId());
            return ConsentResponseDto.from(existingConsent.get(), leadOpt.map(Lead::getId).orElse(null), leadOpt.map(l -> l.getStatus().name()).orElse(null));
        }

        boolean hasActiveLead = leadRepository.existsByBuyerIdAndDealerIdAndQuotationIdAndStatusNot(
                buyer.getId(),
                dealer.getId(),
                quotation.getId(),
                LeadStatus.REVOKED
        );
        if (hasActiveLead) {
            if (existingConsent.isPresent()) {
                Optional<Lead> leadOpt = leadRepository.findByConsentId(existingConsent.get().getId());
                return ConsentResponseDto.from(existingConsent.get(), leadOpt.map(Lead::getId).orElse(null), leadOpt.map(l -> l.getStatus().name()).orElse(null));
            }
            throw new DuplicateResourceException("You already have an active lead with this dealership for this group.");
        }

        QuotationVersion activeVersion;
        if (quotation.getCurrentVersionId() != null) {
            activeVersion = quotationVersionRepository.findById(quotation.getCurrentVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuotationVersion", "id", quotation.getCurrentVersionId()));
        } else {
            List<QuotationVersion> versions = quotationVersionRepository.findByQuotationIdOrderByVersionNumberDesc(quotation.getId());
            if (versions.isEmpty()) {
                throw new ResourceNotFoundException("QuotationVersion", "quotationId", quotation.getId());
            }
            activeVersion = versions.get(0);
        }

        Instant now = Instant.now();
        Consent consent = new Consent(
                null,
                buyer,
                dealer,
                quotation,
                activeVersion,
                ConsentStatus.GRANTED,
                now
        );
        Consent savedConsent = consentRepository.save(consent);

        Lead lead = new Lead(
                null,
                savedConsent,
                dealer,
                buyer,
                quotation,
                activeVersion,
                LeadStatus.DEMO_DELIVERED,
                buyer.getName() != null ? buyer.getName() : "Verified Buyer",
                buyer.getPhone(),
                buyer.getEmail() != null ? buyer.getEmail() : buyer.getPhone() + "@buyer.motorcircle.in",
                group.getCity()
        );
        Lead savedLead = leadRepository.save(lead);

        String eventDescription = "Explicit contact sharing consent granted to " + dealer.getDealershipName() + " for quotation " + quotation.getId();
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            eventDescription += " | Notes: " + request.notes().trim();
        }

        LeadEvent leadEvent = new LeadEvent(
                null,
                savedLead,
                buyer,
                "CONSENT_GRANTED",
                eventDescription
        );
        leadEventRepository.save(leadEvent);

        if (dealer.getUser() != null) {
            notificationService.createNotification(
                    dealer.getUser(),
                    "New Verified Buyer Lead Unlocked",
                    "A buyer in " + group.getCity() + " has accepted your quotation for " + group.getVariant().getName() + ".",
                    "LEAD_DELIVERED",
                    "/dealer/leads"
            );
        }

        notificationService.createNotification(
                buyer,
                "Showroom Appointment Authorized",
                "Your contact authorization for " + dealer.getDealershipName() + " has been confirmed. The dealership has been notified.",
                "CONSENT_GRANTED",
                "/buyer/leads/lead-" + savedConsent.getId()
        );

        log.info("[CONSENT_GRANTED] Consent [{}] and Lead [{}] created for buyer [{}] to dealer [{}]",
                savedConsent.getId(), savedLead.getId(), buyer.getId(), dealer.getId());

        return ConsentResponseDto.from(savedConsent, savedLead.getId(), savedLead.getStatus().name());
    }

    @Transactional
    public void revokeConsent(User buyer, String consentId, RevokeConsentRequest request) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent", "id", consentId));

        if (!consent.getUser().getId().equals(buyer.getId())) {
            throw new UnauthorizedException("You are not authorized to revoke this consent.");
        }

        if (consent.getStatus() == ConsentStatus.REVOKED) {
            throw new BusinessRuleException("CONSENT_ALREADY_REVOKED", "This consent has already been revoked.");
        }

        Instant now = Instant.now();
        consent.setStatus(ConsentStatus.REVOKED);
        consent.setRevokedAt(now);
        consent.setRevocationReason(request.reason());
        consentRepository.save(consent);

        Optional<Lead> optionalLead = leadRepository.findByConsentId(consent.getId());
        if (optionalLead.isPresent()) {
            Lead lead = optionalLead.get();
            lead.setStatus(LeadStatus.REVOKED);
            leadRepository.save(lead);

            LeadEvent leadEvent = new LeadEvent(
                    null,
                    lead,
                    buyer,
                    "CONSENT_REVOKED",
                    "Buyer revoked contact sharing consent. Reason: " + request.reason()
            );
            leadEventRepository.save(leadEvent);

            if (lead.getDealer().getUser() != null) {
                notificationService.createNotification(
                        lead.getDealer().getUser(),
                        "Buyer Consent Revoked",
                        "Buyer revoked consent for lead " + lead.getId() + ". Direct contact details are now restricted.",
                        "CONSENT_REVOKED",
                        "/dealer/leads"
                );
            }

            notificationService.createNotification(
                    buyer,
                    "Consent Revoked",
                    "Your contact sharing consent has been successfully revoked. Dealership access to your contact info is restricted.",
                    "CONSENT_REVOKED",
                    "/buyer/consents"
            );
        }

        log.info("[CONSENT_REVOKED] Consent [{}] revoked by buyer [{}]", consentId, buyer.getId());
    }

    @Transactional(readOnly = true)
    public List<ConsentResponseDto> getBuyerConsents(User buyer) {
        return consentRepository.findByUserIdOrderByGrantedAtDesc(buyer.getId())
                .stream()
                .map(c -> {
                    Optional<Lead> leadOpt = leadRepository.findByConsentId(c.getId());
                    return ConsentResponseDto.from(
                            c,
                            leadOpt.map(Lead::getId).orElse(null),
                            leadOpt.map(l -> l.getStatus().name()).orElse(null)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DealerLeadDto> getDealerLeads(Dealer dealer, LeadStatus status, Pageable pageable) {
        Page<Lead> leads = (status != null)
                ? leadRepository.findByDealerIdAndStatus(dealer.getId(), status, pageable)
                : leadRepository.findByDealerId(dealer.getId(), pageable);

        return leads.map(lead -> {
            List<LeadEventDto> events = leadEventRepository.findByLeadIdOrderByCreatedAtDesc(lead.getId())
                    .stream()
                    .map(LeadEventDto::from)
                    .toList();
            return DealerLeadDto.from(lead, events);
        });
    }

    @Transactional(readOnly = true)
    public DealerLeadDto getDealerLeadDetail(Dealer dealer, String leadId) {
        Lead lead = leadRepository.findByIdAndDealerId(leadId, dealer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));

        List<LeadEventDto> events = leadEventRepository.findByLeadIdOrderByCreatedAtDesc(lead.getId())
                .stream()
                .map(LeadEventDto::from)
                .toList();

        return DealerLeadDto.from(lead, events);
    }

    @Transactional
    public DealerLeadDto updateLeadStatus(Dealer dealer, String leadId, UpdateLeadStatusRequest request) {
        Lead lead = leadRepository.findByIdAndDealerId(leadId, dealer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));

        if (lead.getStatus() == LeadStatus.REVOKED) {
            throw new BusinessRuleException("LEAD_REVOKED", "Cannot update status of a revoked lead.");
        }

        lead.setStatus(request.status());
        Lead saved = leadRepository.save(lead);

        String desc = "Dealership updated lead status to " + request.status();
        if (request.note() != null && !request.note().isBlank()) {
            desc += " | Note: " + request.note().trim();
        }

        User actor = dealer.getUser() != null ? dealer.getUser() : lead.getBuyer();
        LeadEvent leadEvent = new LeadEvent(
                null,
                saved,
                actor,
                "STATUS_UPDATED",
                desc
        );
        leadEventRepository.save(leadEvent);

        if (lead.getBuyer() != null) {
            String statusPretty = request.status().name().replace('_', ' ');
            String noteText = (request.note() != null && !request.note().isBlank()) ? " Note: " + request.note().trim() : "";
            notificationService.createNotification(
                    lead.getBuyer(),
                    "Lead Milestone Updated: " + statusPretty,
                    dealer.getDealershipName() + " updated your appointment status to " + statusPretty + "." + (noteText.isEmpty() ? "" : " " + noteText),
                    "LEAD_STATUS_UPDATED",
                    "/buyer/leads/lead-" + lead.getConsent().getId()
            );
        }

        List<LeadEventDto> events = leadEventRepository.findByLeadIdOrderByCreatedAtDesc(saved.getId())
                .stream()
                .map(LeadEventDto::from)
                .toList();

        log.info("[LEAD_STATUS_UPDATED] Lead [{}] status updated to [{}] by dealer [{}]",
                leadId, request.status(), dealer.getId());

        return DealerLeadDto.from(saved, events);
    }
}
