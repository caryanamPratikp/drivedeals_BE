package in.motorcircle.backend.admin.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminDealerDto(
    String id,
    String dealershipName,
    String brand,
    String city,
    String address,
    String gstin,
    String status,
    BigDecimal rating,
    int totalQuotes,
    int totalLeadsDelivered,
    Instant verifiedAt,
    String ownerName,
    String ownerPhone,
    String ownerEmail
) {}
