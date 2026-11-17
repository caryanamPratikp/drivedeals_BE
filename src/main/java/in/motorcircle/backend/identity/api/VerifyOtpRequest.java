package in.motorcircle.backend.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
    @NotBlank(message = "Phone number is required")
    String phone,

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "6-digit numeric OTP required")
    String otp,

    String name,
    String role,
    String city,
    String dealershipName
) {
    public VerifyOtpRequest(String phone, String otp) {
        this(phone, otp, null, null, null, null);
    }
}
