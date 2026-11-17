package in.motorcircle.backend.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateCarRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String bodyType,
        String imageUrl,
        @NotNull BigDecimal minPrice,
        @NotNull BigDecimal maxPrice,
        List<CreateVariantRequest> variants
) {
    public record CreateVariantRequest(
            @NotBlank String name,
            @NotBlank String fuelType,
            @NotBlank String transmission,
            @NotNull BigDecimal exShowroomPrice,
            String keyFeatures
    ) {}
}
