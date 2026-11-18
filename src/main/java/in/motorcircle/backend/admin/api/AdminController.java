package in.motorcircle.backend.admin.api;

import in.motorcircle.backend.admin.api.dto.*;
import in.motorcircle.backend.admin.application.AdminService;
import in.motorcircle.backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Operations & Governance", description = "Endpoints for platform administrators to monitor platform telemetry, dealers, groups, leads, and audit streams")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get Platform Analytics", description = "Returns aggregated financial KPIs, deal volumes, member counts, and telemetry")
    public ResponseEntity<ApiResponse<AdminAnalyticsDto>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAnalytics()));
    }

    @GetMapping("/dealers")
    @Operation(summary = "List All Dealerships", description = "Returns all OEM dealerships with verification status and submission counts")
    public ResponseEntity<ApiResponse<List<AdminDealerDto>>> getDealers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDealers()));
    }

    @PatchMapping("/dealers/{id}/status")
    @Operation(summary = "Update Dealer Verification Status", description = "Approve, reject, or suspend dealership access")
    public ResponseEntity<ApiResponse<AdminDealerDto>> updateDealerStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateDealerStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateDealerStatus(id, request), "Dealer status updated successfully"));
    }

    @GetMapping("/users")
    @Operation(summary = "List Platform Users", description = "Returns buyers, dealers, and administrators")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers()));
    }

    @GetMapping("/groups")
    @Operation(summary = "List All Buyer Collectives", description = "Returns all buyer groups with current progression stages")
    public ResponseEntity<ApiResponse<List<AdminGroupDto>>> getGroups() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getGroups()));
    }

    @GetMapping("/leads")
    @Operation(summary = "List Global Buyer Leads", description = "Returns unlocked leads across all dealerships")
    public ResponseEntity<ApiResponse<List<AdminLeadDto>>> getLeads() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getLeads()));
    }

    @GetMapping("/audit")
    @Operation(summary = "Get Regulatory Audit Trail", description = "Returns immutable chronological event ledger")
    public ResponseEntity<ApiResponse<List<AdminAuditDto>>> getAuditEvents() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAuditEvents()));
    }
}
