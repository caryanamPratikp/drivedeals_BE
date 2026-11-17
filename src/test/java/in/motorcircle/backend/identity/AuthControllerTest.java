package in.motorcircle.backend.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.motorcircle.backend.identity.api.SendOtpRequest;
import in.motorcircle.backend.identity.api.VerifyOtpRequest;
import in.motorcircle.backend.identity.domain.User;
import in.motorcircle.backend.identity.domain.UserRole;
import in.motorcircle.backend.identity.domain.UserStatus;
import in.motorcircle.backend.identity.infrastructure.UserRepository;
import in.motorcircle.backend.shared.security.JwtTokenProvider;
import in.motorcircle.backend.shared.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testBuyer;

    @BeforeEach
    void setUp() {
        testBuyer = userRepository.findByPhone("+919876543210")
                .orElseGet(() -> userRepository.save(new User(
                        "user_test_buyer",
                        "Rahul Sharma",
                        "rahul.test@example.com",
                        "+919876543210",
                        UserRole.BUYER,
                        "Bengaluru",
                        "560001",
                        UserStatus.ACTIVE
                )));
    }

    @Test
    @DisplayName("POST /auth/otp/send succeeds with valid phone number")
    void testSendOtpValidPhone() throws Exception {
        SendOtpRequest request = new SendOtpRequest("+919876543210");

        mockMvc.perform(post("/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60));
    }

    @Test
    @DisplayName("POST /auth/otp/send fails validation with malformed phone number")
    void testSendOtpMalformedPhone() throws Exception {
        SendOtpRequest request = new SendOtpRequest("12345"); // Missing +91 and 10 digits

        mockMvc.perform(post("/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Parameters"))
                .andExpect(jsonPath("$.validationErrors.phone").exists());
    }

    @Test
    @DisplayName("POST /auth/otp/verify authenticates user and returns JWT access token")
    void testVerifyOtpAndAuthenticate() throws Exception {
        // First send OTP
        mockMvc.perform(post("/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendOtpRequest("+919876543210"))));

        // Verify using dev mock OTP 123456
        VerifyOtpRequest request = new VerifyOtpRequest("+919876543210", "123456");

        mockMvc.perform(post("/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.phone").value("+919876543210"))
                .andExpect(jsonPath("$.data.user.role").value("BUYER"));
    }

    @Test
    @DisplayName("GET /auth/me without Bearer token returns 403 Forbidden")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /auth/me with valid Bearer token returns current user profile")
    void testGetMeAuthenticated() throws Exception {
        UserPrincipal principal = UserPrincipal.create(testBuyer, null);
        String token = jwtTokenProvider.generateAccessToken(principal);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testBuyer.getId()))
                .andExpect(jsonPath("$.data.phone").value(testBuyer.getPhone()))
                .andExpect(jsonPath("$.data.role").value("BUYER"));
    }
}
