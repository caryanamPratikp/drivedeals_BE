package in.motorcircle.backend.group.api;

import in.motorcircle.backend.group.application.BuyerGroupService;
import in.motorcircle.backend.shared.response.ApiResponse;
import in.motorcircle.backend.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups")
@Tag(name = "Demand Groups", description = "Endpoints for buyer group formation, discovery, and membership management")
public class GroupController {

    private final BuyerGroupService groupService;

    public GroupController(BuyerGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    @Operation(summary = "Browse Demand Groups", description = "List active car buying groups filtered by city, brand, or lifecycle stage")
    public ResponseEntity<ApiResponse<Page<BuyerGroupSummaryDto>>> getGroups(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String stage,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BuyerGroupSummaryDto> groups = groupService.getGroups(city, brand, stage, pageable);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Group Details", description = "Returns full group progression, target quotas, and aggregated demand telemetry")
    public ResponseEntity<ApiResponse<BuyerGroupDetailDto>> getGroupById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String currentUserId = principal != null ? principal.getId() : null;
        BuyerGroupDetailDto group = groupService.getGroupById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Initiate New Demand Group", description = "Creates a new buyer group for a vehicle variant and enrolls the creator")
    public ResponseEntity<ApiResponse<BuyerGroupDetailDto>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BuyerGroupDetailDto created = groupService.createGroup(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Demand group initiated successfully"));
    }

    @PostMapping("/{id}/memberships")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Join Demand Group", description = "Enrolls the authenticated buyer into an active group and records their purchase signals")
    public ResponseEntity<ApiResponse<Void>> joinGroup(
            @PathVariable String id,
            @Valid @RequestBody(required = false) JoinGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        groupService.joinGroup(id, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Joined demand group successfully"));
    }

    @DeleteMapping("/{id}/memberships/me")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Leave Demand Group", description = "Withdraws active membership from a demand group")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        groupService.leaveGroup(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/signals")
    @PreAuthorize("hasRole('BUYER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit Demand Signal", description = "Submits or updates purchase timeline, finance mode, and exchange car preferences")
    public ResponseEntity<ApiResponse<Void>> submitSignal(
            @PathVariable String id,
            @Valid @RequestBody CreateSignalPayload payload,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        groupService.submitSignal(id, payload, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Buyer signal updated successfully"));
    }
}
