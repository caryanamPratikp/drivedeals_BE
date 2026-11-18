package in.motorcircle.backend.admin.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminLeadDto(
    String id,
    String dealerId,
    String dealershipName,
    String buyerId,
    String buyerName,
    String buyerPhone,
    String carModel,
    String variantName,
    BigDecimal onRoadPrice,
    String status,
    Instant createdAt
) {}
