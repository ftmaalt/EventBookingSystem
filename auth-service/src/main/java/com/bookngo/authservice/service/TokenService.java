package com.bookngo.authservice.service;

import com.bookngo.authservice.exception.InformationNotFoundException;
import com.bookngo.authservice.model.Tokens;
import com.bookngo.authservice.model.User;
import com.bookngo.authservice.model.enums.TokenType;
import com.bookngo.authservice.repository.TokensRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.bookngo.authservice.model.enums.TokenType.*;


@Service
public class TokenService {

    private final TokensRepository tokensRepository;

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    public TokenService(TokensRepository tokensRepository) {
        this.tokensRepository = tokensRepository;
    }


    public String generateToken(User user, TokenType type) {
        logger.info("Generating {} token for user ID: {}", type, user.getId());
        String tokenString = UUID.randomUUID().toString();
        Tokens token = new Tokens();
        token.setToken(tokenString);
        token.setType(type);
        token.setUser(user);
        if (type == EMAIL_VERIFICATION) {
            token.setExpiresAt(LocalDateTime.now().plusHours(24));
        }else if(type == PASSWORD_RESET){
            token.setExpiresAt(LocalDateTime.now().plusMinutes(6));
        } else if (type == PENALTY_PAYMENT) {
            token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        }
        tokensRepository.save(token);
        logger.info("{} token generated successfully for user ID: {}", type, user.getId());
        return tokenString;
    }

    public User validateToken(String tokenString, TokenType expectedType) {
        logger.info("Validating {} token", expectedType);
        Tokens token = tokensRepository.findByToken(tokenString).orElseThrow(() -> new InformationNotFoundException("Invalid token."));

        if (token.getType() != expectedType) {
            throw new InformationNotFoundException("Invalid token.");
        }
        if (token.getUsedAt() != null) {
            throw new InformationNotFoundException("This token has already been used.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InformationNotFoundException("This token has expired.");
        }
        token.setUsedAt(LocalDateTime.now());
        tokensRepository.save(token);
        logger.info("{} token validated successfully for user ID: {}", expectedType, token.getUser().getId());
        return token.getUser();
    }

}