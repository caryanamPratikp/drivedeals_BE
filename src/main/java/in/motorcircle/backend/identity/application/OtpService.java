package in.motorcircle.backend.identity.application;

public interface OtpService {
    /**
     * Dispatches an OTP to the given phone number with rate limiting and expiry.
     * @param phone E.164 formatted telephone number (+91XXXXXXXXXX)
     * @return retry delay in seconds before another OTP can be requested
     */
    int sendOtp(String phone);

    /**
     * Verifies the provided OTP against the active session for the phone.
     * @param phone E.164 formatted telephone number
     * @param otp 6-digit verification code
     * @return true if valid and consumed; throws BusinessRuleException on mismatch or lockout
     */
    boolean verifyOtp(String phone, String otp);

    /**
     * Retrieves the latest active OTP for dev / test inspection.
     * @param phone E.164 formatted telephone number
     * @return the active 6-digit OTP string or null
     */
    String getLatestOtp(String phone);
}
