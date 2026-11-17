package in.motorcircle.backend.catalog.api;

import java.math.BigDecimal;
import java.util.List;

public record CarDetailDto(
    String id,
    String brand,
    String model,
    String bodyType,
    String imageUrl,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<CarVariantSummaryDto> variants
) {}
