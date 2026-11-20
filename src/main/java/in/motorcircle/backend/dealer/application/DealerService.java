package in.motorcircle.backend.dealer.application;

import in.motorcircle.backend.dealer.api.DealerOnboardingRequest;
import in.motorcircle.backend.dealer.api.DealerProfileDto;
import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.domain.DealerStatus;
import in.motorcircle.backend.dealer.infrastructure.DealerRepository;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.notification.application.NotificationService;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DealerService {

    private final DealerRepository dealerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public DealerService(DealerRepository dealerRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.dealerRepository = dealerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public DealerProfileDto getCurrentDealer(UserPrincipal principal) {
        Dealer dealer = resolveDealer(principal);
        return DealerProfileDto.from(dealer);
    }

    @Transactional
    public DealerProfileDto saveOnboarding(UserPrincipal principal, DealerOnboardingRequest request) {
        Dealer dealer = resolveDealer(principal);

        if (request.dealershipName() != null && !request.dealershipName().trim().isEmpty()) {
            dealer.setDealershipName(request.dealershipName().trim());
        }
        if (request.legalEntityName() != null) {
            dealer.setLegalEntityName(request.legalEntityName().trim());
        }
        if (request.brand() != null && !request.brand().trim().isEmpty()) {
            dealer.setBrand(request.brand().trim());
        }
        if (request.dealershipType() != null) {
            dealer.setDealershipType(request.dealershipType().trim());
        }
        if (request.dealerCode() != null) {
            dealer.setDealerCode(request.dealerCode().trim());
        }
        if (request.gstin() != null) {
            dealer.setGstin(request.gstin().trim().toUpperCase());
        }
        if (request.panNumber() != null) {
            dealer.setPanNumber(request.panNumber().trim().toUpperCase());
        }
        if (request.address() != null) {
            dealer.setAddress(request.address().trim());
        }
        if (request.city() != null) {
            dealer.setCity(request.city().trim());
        }
        if (request.state() != null) {
            dealer.setState(request.state().trim());
        }
        if (request.pincode() != null) {
            dealer.setPincode(request.pincode().trim());
        }
        if (request.contactPerson() != null) {
            dealer.setContactPerson(request.contactPerson().trim());
        }
        if (request.contactEmail() != null) {
            dealer.setContactEmail(request.contactEmail().trim());
        }
        if (request.contactPhone() != null) {
            dealer.setContactPhone(request.contactPhone().trim());
        }

        if (request.isDraft()) {
            dealer.setOnboardingStatus("IN_PROGRESS");
        } else {
            // Final submission
            dealer.setOnboardingStatus("SUBMITTED");
            dealer.setStatus(DealerStatus.PENDING_VERIFICATION);

            // Notify Platform Administrators
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN)
                    .toList();

            String gstinInfo = dealer.getGstin() != null ? " (GSTIN: " + dealer.getGstin() + ")" : "";
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "New Dealership Verification Request",
                        dealer.getDealershipName() + " (" + dealer.getBrand() + ") submitted onboarding details" + gstinInfo + " for verification.",
                        "DEALER_VERIFICATION",
                        "/admin/dealers"
                );
            }
        }

        Dealer saved = dealerRepository.save(dealer);
        return DealerProfileDto.from(saved);
    }

    private Dealer resolveDealer(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        if (principal.getDealerId() != null) {
            return dealerRepository.findById(principal.getDealerId())
                    .orElseGet(() -> dealerRepository.findByUserId(principal.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Dealer record not found for user: " + principal.getId())));
        }
        return dealerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer record not found for user: " + principal.getId()));
    }
}
