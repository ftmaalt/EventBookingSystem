package com.bookngo.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtUtils {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(MyUserDetails myUserDetails){
        return Jwts.builder()
                .subject(myUserDetails.getUsername())
                .claim("uid", myUserDetails.getUser().getId())
                .claim("role", myUserDetails.getUser().getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserNameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getPayload()
                .getSubject();
    }

    public Boolean validateJwtToken(String authToken){
        try{
            Jwts.parser().verifyWith(getSigningKey()).build().parseClaimsJws(authToken);
            return true;

        } catch (Exception e){
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}

