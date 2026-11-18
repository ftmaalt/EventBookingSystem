package com.bookngo.complianceservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
            Number uid = claims.get("uid", Number.class);
            String role = claims.get("role", String.class);
            if (uid == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(uid.longValue(), claims.getSubject(), role));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
