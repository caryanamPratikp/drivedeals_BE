package in.motorcircle.backend.group.api;

import in.motorcircle.backend.catalog.api.CarSummaryDto;
import in.motorcircle.backend.catalog.api.CarVariantSummaryDto;

import java.time.Instant;

public record BuyerGroupSummaryDto(
    String id,
    CarSummaryDto car,
    CarVariantSummaryDto variant,
    String city,
    int targetMembers,
    int currentMembers,
    String stage,
    Instant expiresAt
) {}
