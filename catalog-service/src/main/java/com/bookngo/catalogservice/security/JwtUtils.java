package com.bookngo.catalogservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

/** Validates tokens issued by auth-service (same JWT_SECRET). Never creates tokens. */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));

    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Number userId = claims.get("uid", Number.class);
            String role = claims.get("role", String.class);
            String email = claims.getSubject();

            if (userId == null || userId.longValue() <= 0 || role == null || role.isBlank() || email == null || email.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new AuthenticatedUser(userId.longValue(), email, role));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
