package in.motorcircle.backend.lead.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeConsentRequest(
        @NotBlank(message = "Revocation reason is required")
        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason
) {
}
