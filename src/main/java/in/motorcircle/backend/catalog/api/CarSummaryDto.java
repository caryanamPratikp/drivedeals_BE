package in.motorcircle.backend.catalog.api;

import java.math.BigDecimal;

public record CarSummaryDto(
    String id,
    String brand,
    String model,
    String bodyType,
    String imageUrl,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    int variantCount
) {}
