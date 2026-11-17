package in.motorcircle.backend.identity;

import in.motorcircle.backend.identity.application.DefaultOtpService;
import in.motorcircle.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpServiceTest {

    private DefaultOtpService otpService;
    private final String testPhone = "+919876543210";

    @BeforeEach
    void setUp() {
        otpService = new DefaultOtpService();
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "allowMockOtp", true);
        ReflectionTestUtils.setField(otpService, "mockOtpValue", "123456");
    }

    @Test
    @DisplayName("Send OTP generates session and returns cooldown")
    void testSendOtpSuccess() {
        int cooldown = otpService.sendOtp(testPhone);
        assertThat(cooldown).isEqualTo(60);
    }

    @Test
    @DisplayName("Verify OTP succeeds with mock OTP value in dev mode")
    void testVerifyMockOtp() {
        otpService.sendOtp(testPhone);
        boolean verified = otpService.verifyOtp(testPhone, "123456");
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Invalid OTP throws BusinessRuleException with remaining attempts")
    void testInvalidOtpThrowsException() {
        otpService.sendOtp(testPhone);
        // Turn off mock OTP for this test
        ReflectionTestUtils.setField(otpService, "allowMockOtp", false);

        assertThatThrownBy(() -> otpService.verifyOtp(testPhone, "000000"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid OTP. 2 attempt(s) remaining.");
    }

    @Test
    @DisplayName("Exceeding 3 failed attempts locks out the phone number")
    void testLockoutAfterMaxAttempts() {
        otpService.sendOtp(testPhone);
        ReflectionTestUtils.setField(otpService, "allowMockOtp", false);

        // Attempt 1
        assertThatThrownBy(() -> otpService.verifyOtp(testPhone, "000001")).isInstanceOf(BusinessRuleException.class);
        // Attempt 2
        assertThatThrownBy(() -> otpService.verifyOtp(testPhone, "000002")).isInstanceOf(BusinessRuleException.class);
        // Attempt 3: Triggers lockout
        assertThatThrownBy(() -> otpService.verifyOtp(testPhone, "000003"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Maximum invalid attempts reached");

        // Subsequent attempt blocked by lockout
        assertThatThrownBy(() -> otpService.sendOtp(testPhone))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Too many failed attempts");
    }
}
