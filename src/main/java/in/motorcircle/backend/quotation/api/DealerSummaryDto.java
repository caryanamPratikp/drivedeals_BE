package in.motorcircle.backend.quotation.api;

import java.math.BigDecimal;

public record DealerSummaryDto(
    String id,
    String dealershipName,
    String brand,
    String city,
    BigDecimal rating,
    boolean isVerified,
    String status
) {}
