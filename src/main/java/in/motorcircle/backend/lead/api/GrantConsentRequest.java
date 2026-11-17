package in.motorcircle.backend.lead.api;

import jakarta.validation.constraints.Size;

public record GrantConsentRequest(
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {
}
