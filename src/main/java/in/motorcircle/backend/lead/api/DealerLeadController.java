package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.dealer.domain.Dealer;
import in.motorcircle.backend.dealer.infrastructure.DealerRepository;
import in.motorcircle.backend.lead.application.ConsentAndLeadService;
import in.motorcircle.backend.lead.domain.LeadStatus;
import in.motorcircle.backend.shared.exception.ResourceNotFoundException;
import in.motorcircle.backend.shared.exception.UnauthorizedException;
import in.motorcircle.backend.shared.response.ApiResponse;
import in.motorcircle.backend.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dealer/leads")
@PreAuthorize("hasRole('DEALER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dealer Lead Management", description = "Endpoints for verified dealerships to view and manage unlocked buyer leads with row-level tenancy")
public class DealerLeadController {

    private final ConsentAndLeadService consentAndLeadService;
    private final DealerRepository dealerRepository;

    public DealerLeadController(ConsentAndLeadService consentAndLeadService, DealerRepository dealerRepository) {
        this.consentAndLeadService = consentAndLeadService;
        this.dealerRepository = dealerRepository;
    }

    private Dealer getAuthenticatedDealer(UserPrincipal principal) {
        String dealerId = principal.getDealerId();
        if (dealerId != null) {
            return dealerRepository.findById(dealerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + dealerId));
        }
        return dealerRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated dealer is not associated with a registered dealership profile"));
    }

    @GetMapping
    @Operation(summary = "List Dealer Leads", description = "Returns a paginated list of leads strictly isolated to the calling dealership")
    public ResponseEntity<ApiResponse<Page<DealerLeadDto>>> getLeads(
            @RequestParam(required = false) LeadStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Dealer dealer = getAuthenticatedDealer(principal);
        Page<DealerLeadDto> leads = consentAndLeadService.getDealerLeads(dealer, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Lead Details", description = "Returns full unlocked contact details and complete event history for a lead")
    public ResponseEntity<ApiResponse<DealerLeadDto>> getLeadDetail(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Dealer dealer = getAuthenticatedDealer(principal);
        DealerLeadDto lead = consentAndLeadService.getDealerLeadDetail(dealer, id);
        return ResponseEntity.ok(ApiResponse.success(lead));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Lead Status", description = "Advances lead stage in pipeline (e.g. CONTACTED, TEST_DRIVE_SCHEDULED, BOOKED)")
    public ResponseEntity<ApiResponse<DealerLeadDto>> updateLeadStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateLeadStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Dealer dealer = getAuthenticatedDealer(principal);
        DealerLeadDto updated = consentAndLeadService.updateLeadStatus(dealer, id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Lead status updated successfully"));
    }
}
