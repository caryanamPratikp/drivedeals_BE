package in.motorcircle.backend.lead.api;

import in.motorcircle.backend.lead.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLeadStatusRequest(
        @NotNull(message = "Lead status is required")
        LeadStatus status,

        @Size(max = 500, message = "Status note cannot exceed 500 characters")
        String note
) {
}
