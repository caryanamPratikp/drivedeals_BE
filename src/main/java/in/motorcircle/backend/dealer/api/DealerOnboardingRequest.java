package in.motorcircle.backend.dealer.api;

public record DealerOnboardingRequest(
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
    boolean isDraft
) {}
