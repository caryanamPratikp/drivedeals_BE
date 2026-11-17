package in.motorcircle.backend.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256-compliance";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, 15, "test-issuer");
    }

    @Test
    @DisplayName("Generate access token creates valid verifiable JWT with claims")
    void testGenerateAccessTokenAndValidate() {
        UserPrincipal principal = new UserPrincipal(
                "user_test_1",
                "+919876543210",
                "Rahul Sharma",
                "ROLE_BUYER",
                null
        );

        String token = tokenProvider.generateAccessToken(principal);
        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();

        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user_test_1");
        assertThat(tokenProvider.getPhoneFromToken(token)).isEqualTo("+919876543210");
        assertThat(tokenProvider.getNameFromToken(token)).isEqualTo("Rahul Sharma");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_BUYER");
        assertThat(tokenProvider.getDealerIdFromToken(token)).isNull();
    }

    @Test
    @DisplayName("Dealer access token contains dealerId claim")
    void testDealerAccessTokenClaims() {
        UserPrincipal dealerPrincipal = new UserPrincipal(
                "user_dealer_1",
                "+919876543212",
                "Vikram Malhotra",
                "ROLE_DEALER",
                "dealer_trident_hyundai"
        );

        String token = tokenProvider.generateAccessToken(dealerPrincipal);
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getDealerIdFromToken(token)).isEqualTo("dealer_trident_hyundai");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_DEALER");
    }

    @Test
    @DisplayName("Tampered JWT token fails validation")
    void testTamperedTokenFails() {
        UserPrincipal principal = new UserPrincipal("u1", "+919999999999", "User", "ROLE_BUYER", null);
        String token = tokenProvider.generateAccessToken(principal);
        String tamperedToken = token + "xyz";

        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }
}
