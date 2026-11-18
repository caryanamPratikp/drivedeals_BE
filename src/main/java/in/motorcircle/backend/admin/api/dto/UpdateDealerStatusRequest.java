package in.motorcircle.backend.admin.api.dto;

import in.motorcircle.backend.dealer.domain.DealerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDealerStatusRequest(
    @NotNull(message = "Status is required")
    DealerStatus status,
    String notes
) {}
