package in.motorcircle.backend.identity.api;

public record SendOtpResponse(
    boolean success,
    String message,
    int retryAfterSeconds,
    String devOtp
) {
    public SendOtpResponse(boolean success, String message, int retryAfterSeconds) {
        this(success, message, retryAfterSeconds, null);
    }
}
