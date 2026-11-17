package in.motorcircle.backend.group.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRequest(
    @NotBlank(message = "Variant ID is required")
    String variantId,

    @NotBlank(message = "City is required")
    String city,

    @Min(value = 2, message = "Target members must be at least 2")
    @Max(value = 50, message = "Target members cannot exceed 50")
    int targetMembers,

    @Valid
    CreateSignalPayload signal
) {}
