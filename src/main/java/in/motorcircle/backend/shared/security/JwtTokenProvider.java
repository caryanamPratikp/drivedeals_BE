package in.motorcircle.backend.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long jwtExpirationMinutes;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${motorcircle.jwt.secret:motorcircle-super-secret-jwt-key-for-hmac-sha256-signing-minimum-32-bytes!}") String secret,
            @Value("${motorcircle.auth.jwt-access-expiration-minutes:15}") long jwtExpirationMinutes,
            @Value("${motorcircle.jwt.issuer:motorcircle-api}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMinutes = jwtExpirationMinutes;
        this.issuer = issuer;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(jwtExpirationMinutes * 60);

        return Jwts.builder()
                .subject(principal.getId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .claim("phone", principal.getPhone())
                .claim("name", principal.getName())
                .claim("role", principal.getRole())
                .claim("dealerId", principal.getDealerId())
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    public String getPhoneFromToken(String token) {
        return getClaimsFromToken(token).get("phone", String.class);
    }

    public String getNameFromToken(String token) {
        return getClaimsFromToken(token).get("name", String.class);
    }

    public String getDealerIdFromToken(String token) {
        return getClaimsFromToken(token).get("dealerId", String.class);
    }

    public long getExpirationMinutes() {
        return jwtExpirationMinutes;
    }
}
