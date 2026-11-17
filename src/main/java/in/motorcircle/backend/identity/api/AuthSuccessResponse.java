package in.motorcircle.backend.identity.api;

public record AuthSuccessResponse(
    String tokenType,
    String accessToken,
    long expiresIn,
    UserProfileDto user
) {
    public static AuthSuccessResponse of(String accessToken, long expiresIn, UserProfileDto user) {
        return new AuthSuccessResponse("Bearer", accessToken, expiresIn, user);
    }
}
