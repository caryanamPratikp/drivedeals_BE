package in.motorcircle.backend.dealer.api;

import in.motorcircle.backend.dealer.domain.Dealer;
import java.math.BigDecimal;
import java.time.Instant;

public record DealerProfileDto(
    String id,
    String userId,
    String dealershipName,
    String legalEntityName,
    String brand,
    String dealershipType,
    String dealerCode,
    String gstin,
    String panNumber,
    String address,
    String city,
    String state,
    String pincode,
    String contactPerson,
    String contactEmail,
    String contactPhone,
    String status,
    String onboardingStatus,
    BigDecimal rating,
    int totalQuotes,
    int totalLeadsDelivered,
    Instant verifiedAt,
    Instant createdAt
) {
    public static DealerProfileDto from(Dealer dealer) {
        if (dealer == null) return null;
        return new DealerProfileDto(
            dealer.getId(),
            dealer.getUser() != null ? dealer.getUser().getId() : null,
            dealer.getDealershipName(),
            dealer.getLegalEntityName(),
            dealer.getBrand(),
            dealer.getDealershipType(),
            dealer.getDealerCode(),
            dealer.getGstin(),
            dealer.getPanNumber(),
            dealer.getAddress(),
            dealer.getCity(),
            dealer.getState(),
            dealer.getPincode(),
            dealer.getContactPerson(),
            dealer.getContactEmail(),
            dealer.getContactPhone(),
            dealer.getStatus() != null ? dealer.getStatus().name() : null,
            dealer.getOnboardingStatus(),
            dealer.getRating(),
            dealer.getTotalQuotes(),
            dealer.getTotalLeadsDelivered(),
            dealer.getVerifiedAt(),
            dealer.getCreatedAt()
        );
    }
}
