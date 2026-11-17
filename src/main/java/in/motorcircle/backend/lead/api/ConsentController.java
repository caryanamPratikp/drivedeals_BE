package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.lead.application.ConsentAndLeadService;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.response.ApiResponse;
import in.motorcircle.backend.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Consent & Privacy", description = "DPDP Act compliant explicit buyer contact-sharing consent handshake and revocation")
public class ConsentController {

    private final ConsentAndLeadService consentAndLeadService;
    private final UserRepository userRepository;

    public ConsentController(ConsentAndLeadService consentAndLeadService, UserRepository userRepository) {
        this.consentAndLeadService = consentAndLeadService;
        this.userRepository = userRepository;
    }

    @PostMapping("/quotations/{quotationId}/consent")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Grant Contact Sharing Consent", description = "Captures explicit buyer consent and atomically generates an unlocked dealer lead")
    public ResponseEntity<ApiResponse<ConsentResponseDto>> grantConsent(
            @PathVariable String quotationId,
            @Valid @RequestBody(required = false) GrantConsentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User buyer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        ConsentResponseDto response = consentAndLeadService.grantConsentAndCreateLead(buyer, quotationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Consent granted and dealer lead generated successfully"));
    }

    @GetMapping("/consents/me")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List My Consents", description = "Returns all contact sharing consents granted by the current authenticated buyer")
    public ResponseEntity<ApiResponse<List<ConsentResponseDto>>> getMyConsents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User buyer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        List<ConsentResponseDto> consents = consentAndLeadService.getBuyerConsents(buyer);
        return ResponseEntity.ok(ApiResponse.success(consents));
    }

    @PostMapping("/consents/{id}/revoke")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke Contact Sharing Consent", description = "Withdraws previously granted consent, marks lead as revoked, and restricts dealership access")
    public ResponseEntity<ApiResponse<Void>> revokeConsent(
            @PathVariable String id,
            @Valid @RequestBody RevokeConsentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User buyer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        consentAndLeadService.revokeConsent(buyer, id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Consent revoked successfully"));
    }
}
