package in.motorcircle.backend.identity.api;

public record UserProfileDto(
    String id,
    String name,
    String email,
    String phone,
    String role,
    String city,
    String pincode,
    String dealerId,
    String dealershipName,
    String brand,
    String dealerStatus,
    String onboardingStatus
) {}
