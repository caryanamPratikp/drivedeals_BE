package in.motorcircle.backend.admin.api.dto;

import java.time.Instant;

public record AdminGroupDto(
    String id,
    String carId,
    String carBrand,
    String carModel,
    String variantId,
    String variantName,
    String city,
    int targetMembers,
    int currentMembers,
    String stage,
    Instant expiresAt,
    String createdByName
) {}
