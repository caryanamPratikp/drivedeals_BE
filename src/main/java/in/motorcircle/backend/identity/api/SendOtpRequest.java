package in.motorcircle.backend.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "Valid Indian mobile number with +91 prefix required (e.g. +919876543210)")
    String phone,
    String mode
) {
    public SendOtpRequest(String phone) {
        this(phone, null);
    }
}
