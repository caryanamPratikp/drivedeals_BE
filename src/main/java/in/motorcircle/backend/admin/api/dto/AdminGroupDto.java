package in.motorcircle.backend.admin.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminGroupDto(
    String id,
    String carId,
    String carBrand,
    String carModel,
    String variantId,
    String variantName,
    String city,
    int targetMembers,
    int currentMembers,
    String stage,
    Instant expiresAt,
    String createdByName,
    Instant createdAt,
    List<GroupBuyerSignalDto> buyerSignals,
    List<GroupDealerQuoteDto> dealerQuotes,
    List<GroupMemberDto> members,
    List<GroupActivityDto> activities
) {
    public record GroupBuyerSignalDto(
        String id,
        String buyerId,
        String buyerName,
        String buyerPhone,
        String timelineMonths,
        String purchaseMode,
        boolean exchangeCar,
        String exchangeCarDetails,
        String notes,
        Instant createdAt
    ) {}

    public record GroupDealerQuoteDto(
        String quotationId,
        String dealerId,
        String dealershipName,
        String dealerBrand,
        String dealerCity,
        String dealerPhone,
        int versionNumber,
        BigDecimal exShowroomPrice,
        BigDecimal groupDiscount,
        BigDecimal corporateDiscount,
        BigDecimal exchangeBonus,
        BigDecimal totalOnRoadPrice,
        String accessoryPackage,
        int warrantyYears,
        Instant validUntil,
        int deliveryTimelineWeeks,
        String status,
        Instant createdAt
    ) {}

    public record GroupMemberDto(
        String membershipId,
        String userId,
        String userName,
        String userPhone,
        String userCity,
        Instant joinedAt,
        String status
    ) {}

    public record GroupActivityDto(
        String id,
        String type, // "GROUP_CREATED", "BUYER_JOINED", "BUYER_SIGNAL", "DEALER_QUOTE", "STAGE_UPDATED"
        String actorName,
        String actorRole,
        String title,
        String description,
        Instant timestamp
    ) {}
}
