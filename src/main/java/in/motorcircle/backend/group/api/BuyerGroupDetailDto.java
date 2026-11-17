package in.motorcircle.backend.group.api;

import in.motorcircle.backend.catalog.api.CarDetailDto;
import in.motorcircle.backend.catalog.api.CarVariantSummaryDto;
import in.motorcircle.backend.signal.api.TelemetrySummaryDto;

import java.time.Instant;

public record BuyerGroupDetailDto(
    String id,
    CarDetailDto car,
    CarVariantSummaryDto variant,
    String city,
    int targetMembers,
    int currentMembers,
    String stage,
    Instant expiresAt,
    boolean isCurrentUserMember,
    TelemetrySummaryDto telemetry
) {}
