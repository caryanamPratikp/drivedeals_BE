package in.motorcircle.backend.identity.api;

import in.motorcircle.backend.identity.application.AuthService;
import in.motorcircle.backend.shared.response.ApiResponse;
import in.motorcircle.backend.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication & Identity", description = "Endpoints for passwordless phone OTP authentication and profile management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Request Phone OTP", description = "Dispatches a 6-digit verification code to the given Indian phone number via SMS")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.message()));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP & Authenticate", description = "Validates the OTP and returns a signed JWT access token and user profile")
    public ResponseEntity<ApiResponse<AuthSuccessResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthSuccessResponse response = authService.verifyOtpAndAuthenticate(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns profile details for the authenticated user principal from Bearer token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileDto profile = authService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
