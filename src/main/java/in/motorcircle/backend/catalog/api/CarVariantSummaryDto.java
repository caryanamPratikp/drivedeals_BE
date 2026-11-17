package in.motorcircle.backend.catalog.api;

import java.math.BigDecimal;

public record CarVariantSummaryDto(
    String id,
    String name,
    String fuelType,
    String transmission,
    BigDecimal exShowroomPrice,
    String keyFeatures
) {}
