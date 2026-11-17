package in.motorcircle.backend.quotation.api;

import java.math.BigDecimal;
import java.time.Instant;

public record QuotationVersionDto(
    String id,
    int versionNumber,
    BigDecimal exShowroomPrice,
    BigDecimal registrationCharges,
    BigDecimal insuranceCharges,
    BigDecimal accessoriesCharges,
    BigDecimal otherCharges,
    BigDecimal groupDiscount,
    BigDecimal corporateDiscount,
    BigDecimal exchangeBonus,
    BigDecimal totalOnRoadPrice,
    String accessoryPackage,
    int warrantyYears,
    Instant validUntil,
    int deliveryTimelineWeeks,
    Instant createdAt
) {}
