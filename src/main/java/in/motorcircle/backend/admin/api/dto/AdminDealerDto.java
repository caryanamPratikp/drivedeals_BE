package in.motorcircle.backend.admin.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminDealerDto(
    String id,
    String dealershipName,
    String legalEntityName,
    String brand,
    String city,
    String state,
    String pincode,
    String address,
    String gstin,
    String panNumber,
    String dealerCode,
    String dealershipType,
    String status,
    String onboardingStatus,
    BigDecimal rating,
    int totalQuotes,
    int totalLeadsDelivered,
    Instant verifiedAt,
    String ownerName,
    String ownerPhone,
    String ownerEmail,
    String contactPerson,
    String contactPhone,
    String contactEmail,
    Instant createdAt
) {}
