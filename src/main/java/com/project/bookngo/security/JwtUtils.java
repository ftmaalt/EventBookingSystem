package com.project.bookngo.security;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class JwtUtils {
    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expiration-ms}")
    private int jwtExpirationMs;

    Logger logger= Logger.getLogger(JwtUtils.class.getName());

    public String generateJwtToken(MyUserDetails myUserDetails){
        return Jwts.builder()
                .setSubject((myUserDetails.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime()+jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
    public String getUserNameFromJwtToken(String token){
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Boolean validateJwtToken(String authToken){
        try{
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        }catch (SecurityException e){
            logger.log(Level.SEVERE, "Invalid JWT Signature: {0}", e.getMessage());
        }
        return false;
    }
}

