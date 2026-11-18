package in.motorcircle.backend.quotation.api;

import in.motorcircle.backend.quotation.application.QuotationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Quotations & Bidding", description = "Endpoints for dealer quotation submission, revision versioning, and comparison")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping("/groups/{groupId}/quotations")
    @Operation(summary = "Get Group Quotations", description = "Returns all published dealer quotations for a demand group with version histories")
    public ResponseEntity<ApiResponse<List<QuotationDto>>> getQuotationsForGroup(@PathVariable String groupId) {
        List<QuotationDto> quotations = quotationService.getQuotationsForGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(quotations));
    }

    @GetMapping("/dealer/quotations")
    @PreAuthorize("hasRole('DEALER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get Dealer Quotations", description = "Returns all quotations submitted by the authenticated dealer across all groups")
    public ResponseEntity<ApiResponse<List<QuotationDto>>> getDealerQuotations(@AuthenticationPrincipal UserPrincipal principal) {
        List<QuotationDto> quotations = quotationService.getQuotationsForDealer(principal);
        return ResponseEntity.ok(ApiResponse.success(quotations));
    }

    @GetMapping("/quotations/{id}")
    @Operation(summary = "Get Quotation Details", description = "Returns full quotation line-item breakdown and complete version revision trail")
    public ResponseEntity<ApiResponse<QuotationDto>> getQuotationById(@PathVariable String id) {
        QuotationDto quotation = quotationService.getQuotationById(id);
        return ResponseEntity.ok(ApiResponse.success(quotation));
    }

    @PostMapping("/groups/{groupId}/quotations")
    @PreAuthorize("hasRole('DEALER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit Dealer Quotation", description = "Submits a new structured on-road price quotation for an eligible group")
    public ResponseEntity<ApiResponse<QuotationDto>> submitQuotation(
            @PathVariable String groupId,
            @Valid @RequestBody QuotationSubmissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        QuotationDto created = quotationService.submitQuotation(groupId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Quotation submitted successfully"));
    }

    @PostMapping("/quotations/{id}/versions")
    @PreAuthorize("hasRole('DEALER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish Quotation Revision", description = "Appends a new immutable quotation version (v2, v3) preserving historical offers")
    public ResponseEntity<ApiResponse<QuotationVersionDto>> reviseQuotation(
            @PathVariable String id,
            @Valid @RequestBody QuotationRevisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        QuotationVersionDto revised = quotationService.reviseQuotation(id, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(revised, "Quotation revised successfully"));
    }
}
