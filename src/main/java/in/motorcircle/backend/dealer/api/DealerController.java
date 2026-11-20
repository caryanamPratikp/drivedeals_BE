package in.motorcircle.backend.dealer.api;

import in.motorcircle.backend.dealer.application.DealerService;
import in.motorcircle.backend.shared.response.ApiResponse;
import in.motorcircle.backend.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dealers")
@Tag(name = "Dealer Onboarding & Profile", description = "Endpoints for dealership onboarding, draft persistence, and profile verification")
public class DealerController {

    private final DealerService dealerService;

    public DealerController(DealerService dealerService) {
        this.dealerService = dealerService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DEALER', 'ADMIN')")
    @Operation(summary = "Get Dealership Profile", description = "Retrieves the logged-in dealer profile and onboarding state")
    public ResponseEntity<ApiResponse<DealerProfileDto>> getCurrentDealer(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(dealerService.getCurrentDealer(principal)));
    }

    @PostMapping("/onboarding")
    @PreAuthorize("hasAnyRole('DEALER', 'ADMIN')")
    @Operation(summary = "Submit or Save Dealer Onboarding", description = "Saves draft or finalizes dealership onboarding details for verification")
    public ResponseEntity<ApiResponse<DealerProfileDto>> saveOnboarding(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DealerOnboardingRequest request
    ) {
        DealerProfileDto profile = dealerService.saveOnboarding(principal, request);
        String message = request.isDraft() ? "Onboarding draft saved" : "Dealership details submitted for verification";
        return ResponseEntity.ok(ApiResponse.success(profile, message));
    }
}
